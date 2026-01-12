<template>
  <div class="login-body">
    <!-- 1. 背景装饰 (优化：加深圆球颜色，解决太浅问题) -->
    <div class="circle-1"></div>
    <div class="circle-2"></div>

    <div class="container" :class="{ 'right-panel-active': isSignUp }">

      <!-- 2. 注册表单 (Sign Up) - 无修改，保留原有功能 -->
      <div class="form-container sign-up-container">
        <el-form :model="regForm" class="form" size="large">
          <h1>创建账号</h1>
          <div class="social-container">
            <a href="#" class="social"><el-icon><ChatDotRound /></el-icon></a>
            <a href="#" class="social"><el-icon><Platform /></el-icon></a>
            <a href="#" class="social"><el-icon><Link /></el-icon></a>
          </div>
          <span class="tip-text">请填写以下信息进行注册</span>

          <div class="role-switch">
            <span
                v-for="role in ['STUDENT', 'TEACHER']" :key="role"
                class="role-item" :class="{ active: regForm.role === role }"
                @click="regForm.role = role"
            >
              {{ role === 'STUDENT' ? '学生' : '老师' }}
            </span>
          </div>

          <input v-model="regForm.username" type="text" placeholder="学号 / 工号" />
          <input v-model="regForm.name" type="text" placeholder="真实姓名" />
          <input v-model="regForm.password" type="password" placeholder="密码 (默认123456)" />

          <div class="select-wrapper" v-if="regForm.role === 'STUDENT'">
            <el-select v-model="regForm.className" placeholder="选择班级" style="width: 100%" v-if="classList.length > 0">
              <el-option v-for="c in classList" :key="c.id" :label="c.className" :value="c.className" />
            </el-select>
            <el-select v-else placeholder="加载中..." style="width: 100%" disabled>
              <el-option value="" label="加载中..." />
            </el-select>
          </div>
          <div class="select-wrapper" v-else>
            <el-select v-model="regForm.className" placeholder="选择部门/教研室" style="width: 100%" v-if="departmentList.length > 0">
              <el-option v-for="d in departmentList" :key="d.id" :label="d.departmentName" :value="d.departmentName" />
            </el-select>
            <el-select v-else placeholder="加载中..." style="width: 100%" disabled>
              <el-option value="" label="加载中..." />
            </el-select>
          </div>

          <button type="button" @click="handleRegister" class="main-btn">立即注册</button>
        </el-form>
      </div>

      <!-- 3. 登录表单 (Sign In) - 无修改，保留原有功能 -->
      <div class="form-container sign-in-container">
        <el-form :model="form" class="form" size="large">
          <h1>登录系统</h1>
          <div class="social-container">
            <a href="#" class="social"><el-icon><ChatDotRound /></el-icon></a>
            <a href="#" class="social"><el-icon><Platform /></el-icon></a>
            <a href="#" class="social"><el-icon><Link /></el-icon></a>
          </div>
          <span class="tip-text">选择您的身份进行登录</span>

          <div class="role-switch">
            <span
                v-for="role in ['STUDENT', 'TEACHER', 'ADMIN']" :key="role"
                class="role-item" :class="{ active: form.role === role }"
                @click="form.role = role"
            >
              {{ roleMap[role] }}
            </span>
          </div>

          <input v-model="form.username" type="text" placeholder="账号" />
          <input v-model="form.password" type="password" placeholder="密码" @keyup.enter="handleLogin" />


          <button type="button" @click="handleLogin" class="main-btn" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </el-form>
      </div>

      <!-- 4. 覆盖层容器 (含插画) - 优化：替换丑陋火箭图标，解决多余白色问题 -->
      <div class="overlay-container">
        <div class="overlay">

          <!-- 左侧：去登录 (替换火箭图标为美观的用户登录插画，无多余白色) -->
          <div class="overlay-panel overlay-left">
            <!-- ★ 优化1：替换原有丑陋火箭SVG，使用贴合登录场景的插画，无多余白色边缘 -->
            <svg class="illustration" viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="userGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#f0f9ff" />
                  <stop offset="100%" stop-color="#e6f7ff" />
                </linearGradient>
              </defs>
              <!-- 盾牌背景（贴合登录安全场景，无多余白色） -->
              <path fill="#fff" d="M100,20 L30,70 L30,150 L170,150 L170,70 Z" />
              <path fill="url(#userGradient)" d="M100,30 L38,75 L38,145 L162,145 L162,75 Z" />
              <!-- 用户图标（简约美观，无多余空白） -->
              <circle cx="100" cy="80" r="25" fill="#409EFF" />
              <path fill="#fff" d="M100,110 C70,110 50,140 50,140 L150,140 C150,140 130,110 100,110 Z" />
              <!-- 装饰元素（提升美观度，无多余白色） -->
              <circle cx="70" cy="60" r="5" fill="#42b983" />
              <circle cx="130" cy="60" r="5" fill="#42b983" />
            </svg>
            <h1>欢迎回来！</h1>
            <p>与我们一起开启智慧考勤之旅，保持高效与自律。</p>
            <button class="ghost" @click="isSignUp = false">去登录</button>
          </div>

          <!-- 右侧：去注册 (保留原有插画，无多余修改) -->
          <div class="overlay-panel overlay-right">
            <svg class="illustration" viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
              <circle cx="100" cy="70" r="30" fill="#fff" fill-opacity="0.9"/>
              <path fill="#fff" fill-opacity="0.8" d="M100,110 c-40,0 -70,30 -70,70 h140 c0,-40 -30,-70 -70,-70 z"/>
              <circle cx="150" cy="50" r="10" fill="#FFD700" />
              <circle cx="50" cy="150" r="5" fill="#fff" fill-opacity="0.5" />
            </svg>
            <h1>加入我们</h1>
            <p>输入您的信息，成为智慧校园的一员。</p>
            <button class="ghost" @click="isSignUp = true">去注册</button>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useUserStore } from '../store/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api/auth'
import { getDepartments } from '../api/department'
import request from '../utils/request'

const store = useUserStore()
const router = useRouter()
const loading = ref(false)
const isSignUp = ref(false)
const classList = ref([])
  const departmentList = ref([])

const roleMap = { 'STUDENT': '学生', 'TEACHER': '老师', 'ADMIN': '管理员' }

const form = reactive({ username: '', password: '', role: 'STUDENT' })
const regForm = reactive({ username: '', password: '', name: '', role: 'STUDENT', className: '' })

const handleLogin = async () => {
  if(!form.username || !form.password) return ElMessage.warning('请输入账号和密码')
  loading.value = true
  try {
    await store.loginAction(form)
    ElMessage.success('登录成功')
    router.push('/')
    // eslint-disable-next-line no-empty
  } catch (e) {
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if(!regForm.username) return ElMessage.warning('请输入账号')
  if(regForm.role === 'STUDENT' && !regForm.className) return ElMessage.warning('请选择班级')
  // 对于教师，className字段现在可选，所以不需要验证

  try {
    await register(regForm)
    ElMessage.success('注册成功，请登录')
    isSignUp.value = false
    form.username = regForm.username
    form.role = regForm.role
    // eslint-disable-next-line no-empty
  } catch(e) {}
}

onMounted(async () => {
  try {
    // 加载班级列表
    const classRes = await request.get('/class/list')
    classList.value = classRes || []
    
    // 加载部门列表
    const deptRes = await getDepartments()
    departmentList.value = deptRes || []
    // eslint-disable-next-line no-empty
  } catch(e) {
    console.error('加载班级或部门列表失败:', e)
  }
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css?family=Montserrat:400,800');

/* 1. 基础布局与背景装饰 */
.login-body {
  background: #f6f5f7;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  font-family: 'Montserrat', sans-serif;
  height: 100vh;
  margin: -20px 0 50px;
  position: relative;
  overflow: hidden; /* 防止圆球溢出 */
}

/* 漂浮的圆球装饰 - 优化2：提高opacity值加深颜色，解决太浅问题 */
.circle-1 {
  position: absolute;
  top: -10%;
  left: -10%;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409EFF, #42b983);
  opacity: 0.25; /* 从0.1提高到0.25，颜色更深更明显 */
  animation: float 10s ease-in-out infinite;
}

.circle-2 {
  position: absolute;
  bottom: -15%;
  right: -5%;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: linear-gradient(135deg, #FFD700, #ff9a9e);
  opacity: 0.25; /* 从0.1提高到0.25，颜色更深更明显 */
  animation: float 12s ease-in-out infinite reverse;
}

@keyframes float {
  0% { transform: translate(0, 0); }
  50% { transform: translate(30px, 50px); }
  100% { transform: translate(0, 0); }
}

/* 主容器 */
.container {
  background-color: #fff;
  border-radius: 20px; /* 圆角加大 */
  box-shadow: 0 14px 28px rgba(0,0,0,0.1),
  0 10px 10px rgba(0,0,0,0.08); /* 阴影更柔和 */
  position: relative;
  overflow: hidden;
  width: 900px;
  max-width: 100%;
  min-height: 580px; /* 稍微加高，给插画留空间 */
  z-index: 10;
}

/* 2. 表单通用样式 - 无修改，保留原有样式 */
h1 { font-weight: bold; margin: 0 0 15px; } /* 标题间距 */
p { font-size: 14px; font-weight: 100; line-height: 20px; letter-spacing: 0.5px; margin: 20px 0 30px; }
.tip-text { font-size: 12px; color: #888; margin-bottom: 20px; }

.form-container {
  position: absolute;
  top: 0;
  height: 100%;
  transition: all 0.6s ease-in-out;
}

.form {
  background-color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  padding: 0 50px;
  height: 100%;
  text-align: center;
}

input {
  background-color: #eee;
  border: none;
  padding: 12px 15px;
  margin: 8px 0;
  width: 100%;
  border-radius: 8px; /* 圆角统一 */
  outline: none;
}
/* 下拉框适配 */
.select-wrapper { width: 100%; margin: 8px 0; }
:deep(.el-input__wrapper) { background-color: #eee; box-shadow: none; border-radius: 8px; }
:deep(.el-input__inner) { background-color: #eee; height: 40px; }

/* 社交图标 */
.social-container { margin: 15px 0; }
.social-container a {
  border: 1px solid #DDDDDD;
  border-radius: 50%;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  margin: 0 5px;
  height: 40px;
  width: 40px;
  color: #333;
  text-decoration: none;
  transition: 0.3s;
}
.social-container a:hover { background-color: #409EFF; color: #fff; border-color: #409EFF; }

/* 按钮样式 */
button {
  border-radius: 20px;
  border: 1px solid #409EFF;
  background-color: #409EFF;
  color: #FFFFFF;
  font-size: 12px;
  font-weight: bold;
  padding: 12px 45px;
  letter-spacing: 1px;
  text-transform: uppercase;
  transition: transform 80ms ease-in;
  cursor: pointer;
  margin-top: 20px;
}
button:active { transform: scale(0.95); }
button:focus { outline: none; }
button.ghost { background-color: transparent; border-color: #FFFFFF; margin-top: 5px; }

.forgot-pass { color: #333; font-size: 14px; text-decoration: none; margin: 15px 0; }

/* 3. 面板滑动逻辑 - 无修改，保留原有交互 */
.sign-in-container { left: 0; width: 50%; z-index: 2; }
.sign-up-container { left: 0; width: 50%; opacity: 0; z-index: 1; }

.container.right-panel-active .sign-in-container { transform: translateX(100%); }
.container.right-panel-active .sign-up-container { transform: translateX(100%); opacity: 1; z-index: 5; animation: show 0.6s; }

@keyframes show { 0%, 49.99% { opacity: 0; z-index: 1; } 50%, 100% { opacity: 1; z-index: 5; } }

/* 4. 覆盖层与插画 - 优化3：插画样式微调，确保无多余白色 */
.overlay-container {
  position: absolute;
  top: 0; left: 50%; width: 50%; height: 100%;
  overflow: hidden;
  transition: transform 0.6s ease-in-out;
  z-index: 100;
}

.container.right-panel-active .overlay-container { transform: translateX(-100%); }

.overlay {
  background: linear-gradient(to right, #409EFF, #42b983);
  background-repeat: no-repeat;
  background-size: cover;
  background-position: 0 0;
  color: #FFFFFF;
  position: relative;
  left: -100%; height: 100%; width: 200%;
  transform: translateX(0);
  transition: transform 0.6s ease-in-out;
}

.container.right-panel-active .overlay { transform: translateX(50%); }

.overlay-panel {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  padding: 0 40px;
  text-align: center;
  top: 0; height: 100%; width: 50%;
  transform: translateX(0);
  transition: transform 0.6s ease-in-out;
}

.overlay-left { transform: translateX(-20%); }
.container.right-panel-active .overlay-left { transform: translateX(0); }
.overlay-right { right: 0; transform: translateX(0); }
.container.right-panel-active .overlay-right { transform: translateX(20%); }

/* 插画样式 - 微调确保无多余白色，保持原有动画 */
.illustration {
  width: 150px;
  height: 150px;
  margin-bottom: 20px;
  opacity: 0.9;
  animation: floatIcon 4s ease-in-out infinite;
  /* 额外优化：去除SVG默认空白，确保和背景融合 */
  display: block;
}

@keyframes floatIcon {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

/* 5. 角色选择器 - 无修改，保留原有样式 */
.role-switch {
  background: #f0f0f0;
  border-radius: 20px;
  padding: 4px;
  display: flex;
  margin-bottom: 20px;
  width: 100%;
}
.role-item {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  font-size: 13px;
  cursor: pointer;
  border-radius: 16px;
  color: #666;
  transition: 0.3s;
}
.role-item.active { background: #fff; color: #409EFF; font-weight: bold; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }
</style>