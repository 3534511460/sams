<template>
  <div class="studio-dashboard">

    <!-- 顶部：欢迎栏 (极简风格) -->
    <div class="welcome-header">
      <div class="header-left">
        <h1>个人资料</h1>
        <p class="subtitle">管理您的个人资料和账户设置</p>
      </div>
      <div class="header-right">
        <el-button type="primary" size="large" @click="activeTab = 'info'">
          <el-icon><User /></el-icon> 编辑资料
        </el-button>
      </div>
    </div>

    <!-- 核心布局：Grid System -->
    <div class="dashboard-grid">

      <!-- 左侧：主要内容区 (占 2/3) -->
      <div class="main-column">

        <!-- 个人资料表单 -->
        <div class="studio-card">
          <div class="card-header-row">
            <div class="section-title">个人资料设置</div>
          </div>
          <el-tabs v-model="activeTab" class="custom-tabs">
            <!-- Tab 1: 修改资料 -->
            <el-tab-pane label="基本资料" name="info">
              <div class="form-container">
                <el-form :model="form" label-position="top">
                  <el-form-item label="真实姓名">
                    <el-input v-model="form.name" prefix-icon="User" />
                  </el-form-item>
                  <el-form-item label="班级 / 部门">
                    <el-input
                        v-model="form.className"
                        prefix-icon="School"
                        :disabled="userStore.user.role === 'STUDENT'"
                        :placeholder="userStore.user.role === 'STUDENT' ? '学生无法自行修改班级' : '请输入负责班级'"
                    />
                    <span v-if="userStore.user.role === 'STUDENT'" class="tips">
                      <el-icon><InfoFilled /></el-icon> 如需转班，请联系管理员
                    </span>
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" @click="updateInfo" class="save-btn">保存更改</el-button>
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>

            <!-- Tab 2: 安全中心 -->
            <el-tab-pane label="安全中心" name="password">
              <div class="form-container">
                <el-alert title="建议定期更换密码以保障账户安全" type="info" show-icon :closable="false" style="margin-bottom: 20px;" />
                <el-form :model="pwdForm" :rules="pwdRules" ref="pwdRef" label-position="top">
                  <el-form-item label="当前旧密码" prop="oldPass">
                    <el-input v-model="pwdForm.oldPass" type="password" show-password prefix-icon="Lock" />
                  </el-form-item>
                  <el-form-item label="新密码" prop="newPass">
                    <el-input v-model="pwdForm.newPass" type="password" show-password prefix-icon="Key" />
                  </el-form-item>
                  <el-form-item label="确认新密码" prop="checkPass">
                    <el-input v-model="pwdForm.checkPass" type="password" show-password prefix-icon="Check" />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="danger" @click="updatePassword" class="save-btn">确认修改密码</el-button>
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>

      <!-- 右侧：侧边栏 (占 1/3) -->
      <div class="right-col">
        <!-- 用户信息卡片 -->
        <div class="studio-card side-widget">
          <div class="widget-header">
            <span>用户信息</span>
            <el-icon><User /></el-icon>
          </div>
          <div class="user-profile-summary">
            <div class="avatar-wrapper">
              <el-avatar :size="80" class="user-avatar">
                {{ userStore.user.name ? userStore.user.name.charAt(0) : 'U' }}
              </el-avatar>
            </div>
            <div class="user-identity">
              <h3 class="user-name">{{ userStore.user.name }}</h3>
              <el-tag effect="dark" round :type="roleTypeColor">
                {{ roleName }}
              </el-tag>
            </div>
          </div>
          <div class="user-bio-section">
            <div class="bio-item">
              <el-icon><Postcard /></el-icon>
              <span>账号：{{ userStore.user.username }}</span>
            </div>
            <div class="bio-item">
              <el-icon><School /></el-icon>
              <span>班级：{{ userStore.user.className || '暂无班级信息' }}</span>
            </div>
            <div class="bio-item">
              <el-icon><Calendar /></el-icon>
              <span>注册：{{ userStore.user.createTime || '2025-01-01' }}</span>
            </div>
          </div>
        </div>

        <!-- 快捷操作 -->
        <div class="studio-card side-widget">
          <div class="widget-header">
            <span>快捷操作</span>
            <el-icon><Grid /></el-icon>
          </div>
          <div class="tool-grid">
            <div class="tool-item" @click="activeTab = 'info'">
              <div class="tool-icon" style="background: #E6F4FF; color: #0694A2">
                <el-icon><User /></el-icon>
              </div>
              <span class="tool-name">编辑资料</span>
            </div>
            <div class="tool-item" @click="activeTab = 'password'">
              <div class="tool-icon" style="background: #FFF4E6; color: #FF9F43">
                <el-icon><Lock /></el-icon>
              </div>
              <span class="tool-name">修改密码</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { useUserStore } from '../../store/user'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User,
  School,
  Calendar,
  Postcard,
  InfoFilled,
  Lock,
  Key,
  Check,
  Grid
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const activeTab = ref('info')
const pwdRef = ref(null)

// 角色美化显示
const roleName = computed(() => {
  const map = { 'ADMIN': '超级管理员', 'TEACHER': '教师', 'STUDENT': '在校生' }
  return map[userStore.user.role] || userStore.user.role
})

const roleTypeColor = computed(() => {
  if (userStore.user.role === 'ADMIN') return 'danger'
  if (userStore.user.role === 'TEACHER') return 'warning'
  return 'primary'
})

const form = reactive({
  id: userStore.user.id,
  name: userStore.user.name,
  className: userStore.user.className
})

const pwdForm = reactive({ oldPass: '', newPass: '', checkPass: '' })

const validatePass2 = (rule, value, callback) => {
  if (value !== pwdForm.newPass) callback(new Error('两次输入密码不一致!'))
  else callback()
}

const pwdRules = {
  oldPass: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPass: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '长度至少6位', trigger: 'blur' }],
  checkPass: [{ required: true, validator: validatePass2, trigger: 'blur' }]
}

const updateInfo = async () => {
  await request.post('/user/save', { id: form.id, name: form.name, className: form.className })
  ElMessage.success('保存成功')
  // 更新 Store
  userStore.user.name = form.name
  userStore.user.className = form.className
  localStorage.setItem('user', JSON.stringify(userStore.user))
}

const updatePassword = () => {
  pwdRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await request.post('/user/password', { userId: userStore.user.id, oldPass: pwdForm.oldPass, newPass: pwdForm.newPass })
        ElMessageBox.alert('密码修改成功，请重新登录', '提示', {
          confirmButtonText: '去登录',
          callback: () => userStore.logout()
        })
        // eslint-disable-next-line no-empty
      } catch(e) {}
    }
  })
}
</script>

<style scoped>
/* 全局容器背景 */
.studio-dashboard {
  padding: 10px 20px 40px;
  background-color: transparent; /* 依赖 Layout 的背景 */
  font-family: 'Inter', sans-serif;
  color: #334155;
}

/* 1. 顶部欢迎区 */
.welcome-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 30px;
}

.header-left h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1E293B;
}

.subtitle {
  margin: 5px 0 0;
  font-size: 13px;
  color: #94A3B8;
  font-weight: 500;
}

/* 2. 栅格布局 */
.dashboard-grid {
  display: grid;
  grid-template-columns: 2.5fr 1fr;
  gap: 30px;
}

.main-column {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.right-col {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

/* 3. 卡片样式 */
.studio-card {
  background: #FFFFFF;
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #1E293B;
  margin-bottom: 16px;
}

/* 4. 右侧栏 (工具与统计) */
.side-widget {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.widget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 700;
  color: #334155;
  font-size: 15px;
  margin-bottom: 10px;
}

.tool-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.tool-item {
  background: #F8FAFC;
  border-radius: 16px;
  padding: 15px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.tool-item:hover {
  background: #FFFFFF;
  border-color: #E2E8F0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.tool-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.tool-name {
  font-size: 12px;
  font-weight: 600;
  color: #64748B;
}

/* 用户信息卡片 */
.user-profile-summary {
  text-align: center;
  padding: 20px 0;
}

.user-profile-summary .avatar-wrapper {
  margin: 0 auto 15px;
}

.user-profile-summary .user-avatar {
  border: 3px solid #E2E8F0;
  font-size: 32px;
}

.user-profile-summary .user-name {
  margin: 0 0 10px;
  font-size: 20px;
  font-weight: 600;
  color: #1E293B;
}

.user-bio-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #F1F5F9;
}

.bio-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  color: #64748B;
  font-size: 13px;
}

.bio-item .el-icon {
  margin-right: 8px;
  font-size: 14px;
  color: #94A3B8;
}

/* 表单容器 */
.form-container {
  padding: 10px 0;
  max-width: 450px;
}

.save-btn {
  width: 100%;
  margin-top: 10px;
  border-radius: 8px;
  height: 40px;
  font-size: 15px;
}

.tips {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  display: block;
}

/* 响应式适配 */
@media (max-width: 1200px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>