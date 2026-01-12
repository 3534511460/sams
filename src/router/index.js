import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus' // 导入提示组件

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue'),
        meta: { title: '系统登录' }
    },
    {
        path: '/',
        name: 'Layout',
        component: () => import('../views/Layout.vue'),
        redirect: '/dashboard', // 该路由内部仍默认跳转到仪表盘（保持原有子路由逻辑）
        children: [
            // ============ 1. 数据大屏 ============
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('../views/dashboard/index.vue'),
                meta: { title: '首页数据' }
            },

            // ============ 2. 考勤管理 (老师) ============
            {
                path: 'attendance/publish',
                name: 'TeacherPublish',
                component: () => import('../views/attendance/TeacherPublish.vue'),
                meta: { title: '发起签到' }
            },

            // ============ 3. 考勤管理 (学生) ============
            {
                path: 'attendance/checkin',
                name: 'StudentCheckIn',
                component: () => import('../views/attendance/StudentCheckIn.vue'),
                meta: { title: '我要签到' }
            },

            // ============ 4. 考勤记录 (通用) ============
            {
                path: 'attendance/list',
                name: 'RecordList',
                component: () => import('../views/attendance/RecordList.vue'),
                meta: { title: '考勤记录' }
            },

            // ============ 5. 请假管理 ============
            {
                path: 'leave/audit',
                name: 'AuditLeave',
                component: () => import('../views/leave/AuditLeave.vue'),
                meta: { title: '请假审批' }
            },
            {
                path: 'leave/my',
                name: 'MyLeave',
                component: () => import('../views/leave/MyLeave.vue'),
                meta: { title: '我的请假' }
            },
            // ============ 7. 班级管理 (仅管理员) ============
            {
                path: 'admin/class',
                name: 'ClassManage',
                component: () => import('../views/admin/ClassManage.vue'),
                meta: { title: '班级管理' }
            },
            // ...
            // ============ 8. 日志管理 (管理员) ============
            {
                path: 'admin/log',
                name: 'LogManage',
                component: () => import('../views/admin/LogManage.vue'),
                meta: { title: '操作日志' }
            },
            // ============ 8. 排课管理 (管理员专属) ============
            {
                path: 'admin/schedule',
                name: 'ScheduleManage',
                component: () => import('../views/admin/ScheduleManage.vue'),
                meta: { title: '排课管理' }
            },

            // ============ 9. 我的课表 (全员通用) ============
            {
                path: 'schedule/my',
                name: 'MySchedule',
                component: () => import('../views/schedule/MySchedule.vue'),
                meta: { title: '我的课表' }
            },
            {
                path: 'user/profile',
                name: 'UserProfile',
                component: () => import('../views/user/Profile.vue'),
                meta: { title: '个人中心' }
            },
            // ============ 6. 用户管理 (管理员专属) ============
            {
                path: 'user/manage',
                name: 'UserManage',
                component: () => import('../views/user/UserManage.vue'),
                meta: {
                    title: '用户管理',
                    requireAdmin: true // 新增：标记需要管理员权限
                }
            }
        ]
    },
    // 核心修改1：新增全局重定向规则，匹配所有未定义的根路径/默认路径，跳转到登录页
    {
        path: '/:pathMatch(.*)', // 匹配所有路径（兼容直接访问根路径 / 和其他未匹配路径）
        redirect: '/login' // 全局默认跳转到登录页面
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 全局路由守卫：登录拦截 + 管理员权限拦截 + 标签页标题更新
router.beforeEach((to, from, next) => {
    const userStr = localStorage.getItem('user')
    let user = null

    // 优化：捕获 JSON 解析异常，避免程序崩溃
    try {
        if (userStr) {
            user = JSON.parse(userStr)
        }
    } catch (e) {
        console.error('用户信息解析失败：', e)
        localStorage.removeItem('user') // 清除异常的用户信息
        user = null
    }

    // 优化1：已登录用户访问登录页，自动跳转到首页
    if (to.path === '/login' && userStr) {
        next('/dashboard')
        return
    }

    // 1. 未登录拦截：非登录页 + 无用户信息，强制跳转登录
    if (to.path !== '/login' && !userStr) {
        ElMessage.warning('请先登录系统')
        next('/login')
        return
    }

    // 2. 管理员权限拦截：访问需要管理员权限的页面，但角色不是ADMIN
    if (to.meta.requireAdmin) {
        // 优化：可选链 + 非空判断，避免报错
        if (!user || user?.role !== 'ADMIN') {
            ElMessage.warning('仅管理员可访问该页面！')
            next('/dashboard') // 跳回首页
            return
        }
    }

    // 优化2：动态更新浏览器标签页标题
    if (to.meta.title) {
        document.title = to.meta.title + ' - 学生考勤系统' // 拼接系统名称，更规范
    }

    next()
})

export default router