<template>
  <div class="header-container">
    <div class="left">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="right">
      <!-- 消息铃铛 -->
      <div class="msg-box" @click="drawerVisible = true">
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="badge-item">
          <el-icon :class="{'shake-anim': unreadCount > 0}"><Bell /></el-icon>
        </el-badge>
      </div>

      <!-- 用户头像 -->
      <el-dropdown trigger="click" @command="handleCommand">
        <span class="el-dropdown-link user-info">
          <el-avatar :size="30" class="avatar" :style="{ background: generateColor(userStore.user.name) }">
            {{ userStore.user.name ? userStore.user.name.charAt(0) : 'U' }}
          </el-avatar>
          <span class="username">{{ userStore.user.name }}</span>
          <el-tag size="small" effect="plain" class="role-tag">{{ userStore.user.role }}</el-tag>
          <el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item command="logout" style="color: #F56C6C;">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 消息抽屉 -->
    <el-drawer v-model="drawerVisible" title="站内通知" size="350px">
      <div class="drawer-actions">
        <el-button type="primary" link @click="handleReadAll">全部已读</el-button>
        <el-button type="info" link @click="fetchMessages">刷新</el-button>
      </div>
      <!-- ★ 核心修改：msg-list 部分 -->
      <div class="msg-list" v-loading="loading">
        <div
            v-for="msg in msgList"
            :key="msg.id"
            class="msg-card"
            :class="{ 'is-read': msg.isRead === 1, 'is-link': msg.type !== 'NOTICE' }"
            @click="handleMessageClick(msg)"
        >
          <div class="msg-time">{{ formatTime(msg.createTime) }}</div>
          <div class="msg-content">{{ msg.content }}</div>
          <div class="msg-status" v-if="msg.isRead===0"><span class="dot"></span> 未读</div>
          <!-- ★ 新增：可点击提示 -->
          <div class="msg-action" v-if="msg.type !== 'NOTICE'">
            <span>点击处理</span> <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
        <el-empty v-if="msgList.length === 0" description="暂无消息" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '../store/user'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import request from '../utils/request'
import { formatDate } from '../utils/date'
// ★ 新增：导入 ArrowRight 图标
import { Bell, ArrowDown, ArrowRight } from '@element-plus/icons-vue'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const unreadCount = ref(0)
const drawerVisible = ref(false)
const msgList = ref([])
const loading = ref(false)
let timer = null

const generateColor = (name) => {
  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']
  if(!name) return colors[0]
  return colors[name.charCodeAt(0) % colors.length]
}
const formatTime = (val) => val ? formatDate(val) : ''
const fetchUnread = async () => {
  if (!userStore.user.id) return
  // eslint-disable-next-line no-empty
  try { const res = await request.get('/message/unreadCount', { params: { userId: userStore.user.id } }); unreadCount.value = res } catch(e) {}
}
const fetchMessages = async () => {
  loading.value = true
  try {
    const res = await request.get('/message/list', {params: {userId: userStore.user.id}});
    msgList.value = res
  } finally {
    loading.value = false
  }
}
const handleReadAll = async () => {
  await request.post('/message/readAll', {receiverId: userStore.user.id});
  unreadCount.value = 0;
  fetchMessages()
}
const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出吗?', '提示', {type: 'warning'}).then(() => userStore.logout()).catch(() => {
    })
  } else if (command === 'profile') {
    router.push('/user/profile')
  }
}

// ★ 核心新增：消息点击处理函数
const handleMessageClick = async (msg) => {
  // 1. 如果是未读状态，调用接口设为已读，并更新 UI
  if (msg.isRead === 0) {
    try {
      // 调用后端接口
      await request.post('/message/read', { id: msg.id })

      // ★ 前端立刻更新 UI (不用等刷新)
      msg.isRead = 1 // 让卡片变灰
      unreadCount.value = Math.max(0, unreadCount.value - 1) // 让铃铛红点减 1
    } catch (e) {
      console.error(e)
    }
  }

  // 2. 执行路由跳转 (逻辑保持不变)
  if (msg.type === 'LEAVE_AUDIT') {
    router.push({ path: '/leave/audit', query: { highlightId: msg.relationId } })
    drawerVisible.value = false
  }
  else if (msg.type === 'APPEAL_AUDIT') {
    router.push({ path: '/attendance/list', query: { highlightId: msg.relationId } })
    drawerVisible.value = false
  }
}

onMounted(() => {
  fetchUnread();
  timer = setInterval(fetchUnread, 5000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
/* 原有样式保持不变 */
.header-container {
  width: 100%; /* 撑满 */
  height: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  box-sizing: border-box; /* 防止padding把宽度撑爆 */
}

.right {
  display: flex;
  align-items: center;
  gap: 25px;
}

.msg-box {
  cursor: pointer;
  display: flex;
  align-items: center;
}

.msg-box .el-icon {
  font-size: 22px;
  color: #606266;
}

.shake-anim {
  animation: bellShake 2s infinite;
  color: #F56C6C !important;
}

@keyframes bellShake {
  0%, 100% {
    transform: rotate(0);
  }
  10%, 30%, 50%, 70%, 90% {
    transform: rotate(10deg);
  }
  20%, 40%, 60%, 80% {
    transform: rotate(-10deg);
  }
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #333;
}

.avatar {
  margin-right: 8px;
  font-size: 14px;
  font-weight: bold;
  color: #fff;
}

.username {
  font-weight: 500;
  margin-right: 8px;
  font-size: 14px;
}

.role-tag {
  margin-right: 5px;
  background: transparent;
  border: 1px solid #dcdfe6;
  color: #909399;
}

/* 抽屉样式 */
.drawer-actions {
  text-align: right;
  margin-bottom: 10px;
  border-bottom: 1px solid #eee;
  padding-bottom: 5px;
}

.msg-card {
  padding: 12px;
  margin-bottom: 10px;
  border-radius: 8px;
  background: #f0f9eb;
  border-left: 4px solid #67C23A;
  transition: 0.3s;
}

.msg-card.is-read {
  background: #f7f7f7;
  border-left: 4px solid #ccc;
  opacity: 0.8;
}

.msg-time {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.msg-content {
  font-size: 13px;
  color: #333;
  line-height: 1.4;
}

.msg-status {
  font-size: 12px;
  color: #F56C6C;
  margin-top: 5px;
  display: flex;
  align-items: center;
}

.dot {
  width: 6px;
  height: 6px;
  background: #F56C6C;
  border-radius: 50%;
  margin-right: 4px;
}

/* ★ 核心新增：可点击消息样式 */
.msg-card.is-link {
  cursor: pointer;
}

.msg-card.is-link:hover {
  background-color: #ecf5ff;
  border-left-color: #409EFF;
}

.msg-action {
  font-size: 12px;
  color: #409EFF;
  text-align: right;
  margin-top: 5px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
}
</style>