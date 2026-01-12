<template>
  <el-container class="layout-container">
    <!-- 左侧菜单 -->
    <el-aside width="240px" class="aside-menu">
      <div class="logo-area">
        <div class="logo-icon"><el-icon><School /></el-icon></div>
        <span class="logo-text">SAMS 智慧考勤</span>
      </div>

      <!-- ★★★ 注意：所有的 el-menu-item 必须包裹在 el-menu 标签内部！ ★★★ -->
      <el-menu
          router
          :default-active="$route.path"
          class="custom-menu"
          text-color="#606266"
          active-text-color="#409EFF"
      >
        <!-- 1. 首页 -->
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>数据概览</span>
        </el-menu-item>

        <!-- 2. 教务管理 (老师/管理员) -->
        <div class="menu-group-title" v-if="['TEACHER', 'ADMIN'].includes(userRole)">教务管理</div>
        <el-menu-item index="/attendance/publish" v-if="['TEACHER', 'ADMIN'].includes(userRole)">
          <el-icon><Timer /></el-icon><span>发起签到</span>
        </el-menu-item>
        <el-menu-item index="/leave/audit" v-if="['TEACHER', 'ADMIN'].includes(userRole)">
          <el-icon><Stamp /></el-icon><span>请假审批</span>
        </el-menu-item>

        <!-- 3. 学生服务 (学生) -->
        <div class="menu-group-title" v-if="userRole === 'STUDENT'">学生服务</div>
        <el-menu-item index="/attendance/checkin" v-if="userRole === 'STUDENT'">
          <el-icon><CircleCheck /></el-icon><span>我要签到</span>
        </el-menu-item>
        <el-menu-item index="/leave/my" v-if="userRole === 'STUDENT'">
          <el-icon><Reading /></el-icon><span>我的请假</span>
        </el-menu-item>

        <!-- 4. 数据查询 (全员) -->
        <div class="menu-group-title">数据查询</div>
        <el-menu-item index="/attendance/list">
          <el-icon><List /></el-icon><span>考勤记录</span>
        </el-menu-item>
        <el-menu-item index="/schedule/my">
          <el-icon><Calendar /></el-icon><span>我的课表</span>
        </el-menu-item>

        <!-- 5. 系统设置 (管理员) -->
        <div class="menu-group-title" v-if="userRole === 'ADMIN'">系统设置</div>
        <el-menu-item index="/admin/class" v-if="userRole === 'ADMIN'">
          <el-icon><OfficeBuilding /></el-icon><span>班级管理</span>
        </el-menu-item>
        <el-menu-item index="/user/manage" v-if="userRole === 'ADMIN'">
          <el-icon><UserFilled /></el-icon><span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/schedule" v-if="userRole === 'ADMIN'">
          <el-icon><Grid /></el-icon><span>排课管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/log" v-if="userRole === 'ADMIN'">
          <el-icon><Monitor /></el-icon><span>操作日志</span>
        </el-menu-item>

      </el-menu> <!-- ★★★ 闭合标签必须在所有 Item 的最后面！ ★★★ -->
    </el-aside>

    <el-container>
      <!-- 强制白色背景和无内边距 -->
      <el-header class="main-header" style="padding: 0 !important; background: #fff !important; height: 64px; border-bottom: 1px solid #eee;">
        <Header />
      </el-header>

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <!-- AI智能助手 -->
    <AiAssistant />
  </el-container>
</template>

<script setup>
/* eslint-disable vue/multi-word-component-names */
import { computed } from 'vue'
import { useUserStore } from '../store/user'
import Header from '../components/Header.vue'
import AiAssistant from '../components/AiAssistant.vue'

const userStore = useUserStore()
const userRole = computed(() => userStore.user.role)
</script>

<style scoped>
.layout-container { height: 100vh; background-color: #f3f4f6; }

/* 侧边栏样式 */
.aside-menu {
  background-color: #fff;
  border-right: 1px solid #f0f0f0;
  box-shadow: 2px 0 8px rgba(0,0,0,0.02);
  z-index: 10;
  display: flex;
  flex-direction: column;
}
.logo-area { height: 64px; display: flex; align-items: center; justify-content: center; gap: 10px; border-bottom: 1px solid #f5f5f5; }
.logo-icon { width: 36px; height: 36px; background: linear-gradient(135deg, #409EFF, #42b983); border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; box-shadow: 0 4px 10px rgba(64, 158, 255, 0.3); }
.logo-text { font-size: 18px; font-weight: 800; color: #333; letter-spacing: 1px; }

.custom-menu { border-right: none !important; padding: 10px; }
.menu-group-title { padding: 15px 0 5px 20px; font-size: 12px; color: #909399; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
:deep(.el-menu-item) { border-radius: 8px; margin-bottom: 5px; height: 50px; font-weight: 500; }
:deep(.el-menu-item:hover) { background-color: #f0f7ff !important; color: #409EFF; }
:deep(.el-menu-item.is-active) { background: linear-gradient(90deg, #ecf5ff 0%, #fff 100%); color: #409EFF; border-left: 4px solid #409EFF; font-weight: 600; }
:deep(.el-menu-item .el-icon) { font-size: 18px; margin-right: 12px; }

.main-content { padding: 20px; overflow-x: hidden; }

/* 路由动画 */
.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.3s ease; }
.fade-slide-enter-from { opacity: 0; transform: translateX(10px); }
.fade-slide-leave-to { opacity: 0; transform: translateX(-10px); }
</style>