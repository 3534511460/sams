<template>
  <div class="studio-dashboard">

    <!-- 顶部：欢迎栏 (极简风格) -->
    <div class="welcome-header">
      <div class="header-left">
        <h1>我的课表</h1>
        <p class="subtitle">查看本周课程安排</p>
      </div>
      <div class="header-right">
        <el-button type="primary" size="large" @click="refreshData">
          <el-icon><Refresh /></el-icon> 刷新课表
        </el-button>
      </div>
    </div>

    <!-- 核心布局：Grid System -->
    <div class="dashboard-grid">

      <!-- 左侧：主要内容区 (占 2/3) -->
      <div class="main-column">

        <!-- 课表列表 -->
        <div class="studio-card">
          <div class="card-header-row">
            <div class="section-title">课程安排</div>
          </div>
          <TimeTable :data="scheduleData" :can-edit="false" />
        </div>
      </div>

      <!-- 右侧：侧边栏 (占 1/3) -->
      <div class="right-col">
        <!-- 快捷操作 -->
        <div class="studio-card side-widget">
          <div class="widget-header">
            <span>快捷操作</span>
            <el-icon><Grid /></el-icon>
          </div>
          <div class="tool-grid">
            <div class="tool-item" @click="refreshData">
              <div class="tool-icon" style="background: #F3F4F6; color: #64748B">
                <el-icon><Refresh /></el-icon>
              </div>
              <span class="tool-name">刷新课表</span>
            </div>
            <div class="tool-item" @click="$router.push('/attendance/checkin')">
              <div class="tool-icon" style="background: #E6F4FF; color: #0694A2">
                <el-icon><Position /></el-icon>
              </div>
              <span class="tool-name">签到打卡</span>
            </div>
          </div>
        </div>

        <!-- 课程统计信息 -->
        <div class="studio-card side-widget">
          <div class="widget-header">
            <span>课程统计</span>
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="ai-grid">
            <div class="ai-stat-box">
              <span class="label">总课程数</span>
              <span class="val primary">{{ scheduleData.length }}</span>
            </div>
            <div class="ai-stat-box">
              <span class="label">今日课程</span>
              <span class="val warning">{{ todayCourseCount }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import TimeTable from '../../components/TimeTable.vue'
import request from '../../utils/request'
import { useUserStore } from '../../store/user'
import {
  Calendar,
  Refresh,
  Grid,
  DataAnalysis,
  Position
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const scheduleData = ref([])

const todayCourseCount = computed(() => {
  const today = new Date().getDay()
  // 将周日(0)转换为7，以便与weekDay字段匹配
  const normalizedToday = today === 0 ? 7 : today
  return scheduleData.value.filter(course => {
    return course.weekDay === normalizedToday
  }).length
})

const refreshData = async () => {
  try {
    const res = await request.get('/schedule/my', { params: { userId: userStore.user.id } })
    scheduleData.value = res || []
  } catch (error) {
    console.error('获取课表数据失败:', error)
  }
}

onMounted(async () => {
  await refreshData()
})
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

/* 统计卡片 */
.ai-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.ai-stat-box {
  background: #FFFFFF;
  border: 1px solid #F1F5F9;
  border-radius: 16px;
  padding: 15px;
  text-align: center;
}

.ai-stat-box .label {
  display: block;
  font-size: 12px;
  color: #94A3B8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 5px;
}

.ai-stat-box .val {
  font-size: 20px;
  font-weight: 700;
}

.val.primary {
  color: #0694A2;
}

.val.warning {
  color: #FF9F43;
}

.val.danger {
  color: #FF6B6B;
}

/* 响应式适配 */
@media (max-width: 1200px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>