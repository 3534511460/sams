<template>
  <div class="studio-dashboard">

    <!-- 顶部：欢迎栏 (极简风格) -->
    <div class="welcome-header">
      <div class="header-left">
        <h1>{{ greetingText }}，{{ userStore.user.name }}</h1>
        <p class="subtitle">{{ todayInfo }} · <span class="weather">{{ weatherIcon }} 祝您度过富有创意的一天！</span></p>
      </div>
      <div class="header-right">
        <!-- 顶部通知 (悬浮胶囊样式) -->
        <transition name="el-zoom-in-top">
          <div v-if="activeSessions.length > 0" class="active-capsule">
            <div class="capsule-icon"><el-icon class="is-loading"><Loading /></el-icon></div>
            <div class="capsule-info">
              <span class="label">进行中:</span>
              <span class="value">{{ activeSessions[0].courseName }}</span>
              <span class="code">{{ activeSessions[0].checkCode }}</span>
            </div>
            <!-- 修复：确保el-button标签完整闭合 -->
            <el-button type="primary" circle size="small" @click="$router.push('/attendance/checkin')">
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </transition>
      </div>
    </div>

    <!-- 核心布局：Grid System -->
    <div class="dashboard-grid">

      <!-- 左侧：主要内容区 (占 2/3) -->
      <div class="main-column">

        <!-- 1. 我的数据面板 (统计数据 - 设计项目卡片风格) -->
        <div class="stats-container">
          <div class="section-title">我的数据面板</div>
          <div class="stats-row">
            <div v-for="(card, index) in statCards" :key="index" class="project-card" :class="'theme-' + index">
              <div class="card-top">
                <div class="icon-box"><el-icon><component :is="card.icon" /></el-icon></div>
                <div class="trend-badge" v-if="card.trend">
                  <el-icon><TopRight /></el-icon> {{ card.trend }}%
                </div>
              </div>
              <div class="card-content">
                <div class="card-label">{{ card.label }}</div>
                <div class="card-value">{{ card.value }}<span class="unit">{{ card.unit }}</span></div>
              </div>
              <!-- 装饰性波纹 -->
              <svg class="card-wave" viewBox="0 0 1440 320" preserveAspectRatio="none">
                <path fill="rgba(255,255,255,0.3)" d="M0,224L48,213.3C96,203,192,181,288,181.3C384,181,480,203,576,224C672,245,768,267,864,250.7C960,235,1056,181,1152,165.3C1248,149,1344,171,1392,181.3L1440,192L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z"></path>
              </svg>
            </div>
          </div>
        </div>

        <!-- 2. AI 分析报告 (大卡片) -->
        <div class="analysis-section">
          <div class="card-header-row">
            <div class="section-title">AI 智能分析报告</div>
            <!-- 修复：确保el-button标签完整闭合，规范属性写法 -->
            <el-button text bg circle size="small" @click="refreshAnalysis" :loading="aiLoading">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>

          <div class="studio-card ai-card" v-loading="aiLoading">
            <div class="ai-header">
              <div class="ai-avatar">
                <el-icon><MagicStick /></el-icon>
              </div>
              <div class="ai-bubble">
                {{ aiAnalysis.summary || 'AI 正在分析您近期的表现...' }}
              </div>
            </div>

            <div class="ai-grid">
              <div class="ai-stat-box">
                <span class="label">出勤率</span>
                <span class="val primary">{{ aiAnalysis.stats?.attendanceRate || 0 }}%</span>
              </div>
              <div class="ai-stat-box">
                <span class="label">缺勤次数</span>
                <span class="val danger">{{ aiAnalysis.stats?.absent || 0 }}</span>
              </div>
              <div class="ai-stat-box">
                <span class="label">表现趋势</span>
                <span class="val warning">{{ aiAnalysis.trend || '稳定' }}</span>
              </div>
            </div>

            <!-- 风险提示条 -->
            <div class="ai-risk-bar" v-if="aiAnalysis.risks && aiAnalysis.risks.length > 0 && aiAnalysis.risks[0] !== '暂无风险提示，继续保持'">
              <el-icon><Warning /></el-icon>
              <span>{{ aiAnalysis.risks[0] }}</span>
            </div>
          </div>
        </div>

        <!-- 3. 图表区域 -->
        <div class="charts-row">
          <div class="studio-card chart-container">
            <div class="card-title">出勤活跃度热力图</div>
            <div id="barChart" style="height: 280px; width: 100%;"></div>
          </div>
          <div class="studio-card chart-container">
            <div class="card-title">出勤构成分析</div>
            <div id="pieChart" style="height: 280px; width: 100%;"></div>
          </div>
        </div>
      </div>

      <!-- 右侧：侧边栏 (占 1/3) -->
      <div class="right-col">

        <!-- 4. 快捷操作 (模拟 Team Members 布局) -->
        <div class="studio-card side-widget">
          <div class="widget-header">
            <span>快捷操作</span>
            <el-icon><Grid /></el-icon>
          </div>
          <div class="tool-grid">
            <div class="tool-item" v-for="(item, index) in quickEntries" :key="index" @click="$router.push(item.path)">
              <div class="tool-icon" :style="{ background: item.bgColor, color: item.iconColor }">
                <el-icon><component :is="item.icon" /></el-icon>
              </div>
              <span class="tool-name">{{ item.label }}</span>
            </div>
          </div>
        </div>

        <!-- 5. 日程安排 (时间轴) -->
        <div class="studio-card side-widget">
          <div class="widget-header">
            <span>日程与通知</span>
            <el-tag size="small" type="info" effect="plain">今日</el-tag>
          </div>
          <div class="timeline-box">
            <!-- 渲染今日课程安排 -->
            <div v-for="event in scheduleEvents" :key="'event-' + event.time + event.title" class="timeline-item">
              <div class="tl-left">{{ event.time }}</div>
              <div class="tl-content blue-bg">
                <h4>{{ event.title }}</h4>
                <p>{{ event.description }}</p>
              </div>
            </div>
            
            <!-- 渲染系统通知 -->
            <div v-for="notification in notifications" :key="'notification-' + notification.title" class="timeline-item">
              <div class="tl-left">{{ notification.time }}</div>
              <div class="tl-content gray-bg">
                <h4>{{ notification.title }}</h4>
                <p>{{ notification.description }}</p>
              </div>
            </div>
            
            <!-- 如果没有数据，显示空状态 -->
            <div v-if="scheduleEvents.length === 0 && notifications.length === 0" class="timeline-item">
              <div class="tl-left">-</div>
              <div class="tl-content">
                <h4>暂无日程安排</h4>
                <p>今日暂无课程安排或系统通知</p>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import {onMounted, reactive, ref, nextTick, computed} from 'vue'
import * as echarts from 'echarts'
// 引入Element Plus图标（确保图标组件正确导入）
import {
  Loading, ArrowRight, TopRight, MagicStick, Warning, Refresh,
  Grid, User, Avatar, DataLine, PieChart, List, TrendCharts,
  Check, Timer, Position, Memo, CoffeeCup, Calendar, Promotion,
  Stamp, DataAnalysis, Setting
} from '@element-plus/icons-vue'
import {getStatsCounts, getCourseRank} from '../../api/stats'
import {getActiveSessions} from '../../api/attendance'
import {useUserStore} from '../../store/user'
import request from '../../utils/request'

const userStore = useUserStore()
const activeSessions = ref([])
const scheduleEvents = ref([])
const notifications = ref([])
const counts = reactive({
  studentCount: 0, teacherCount: 0, checkInCount: 0,
  attendanceRate: 0, normalCount: 0, lateCount: 0
})

// AI与基础数据
const aiLoading = ref(false)
const aiAnalysis = reactive({summary: '', stats: null, trend: '', suggestions: [], risks: []})

// 文案计算 - 替换为中文问候语
const greetingText = computed(() => {
  const hour = new Date().getHours()
  return hour < 12 ? '早上好' : (hour < 18 ? '下午好' : '晚上好')
})
const todayInfo = computed(() => {
  // 将英文日期格式改为中文格式：2026年1月7日 星期三
  const date = new Date()
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  const weekDay = weekDays[date.getDay()]
  return `${year}年${month}月${day}日 ${weekDay}`
})
const weatherIcon = computed(() => new Date().getHours() < 18 ? '☀️' : '🌙')

// 1. 卡片数据配置 (配色映射)
const statCards = computed(() => {
  const common = [
    {label: '学生总数', value: counts.studentCount, icon: 'User', unit: '', trend: 12},
    {label: '教师总数', value: counts.teacherCount, icon: 'Avatar', unit: '', trend: 5},
    {label: '签到总次数', value: counts.checkInCount, icon: 'DataLine', unit: '', trend: 8},
    {label: '平均出勤率', value: counts.attendanceRate, icon: 'PieChart', unit: '%', trend: 2}
  ]
  const student = [
    {label: '我的签到次数', value: counts.checkInCount, icon: 'List', unit: '', trend: 0},
    {label: '我的出勤率', value: counts.attendanceRate, icon: 'TrendCharts', unit: '%', trend: 0},
    {label: '正常签到', value: counts.normalCount || 0, icon: 'Check', unit: '', trend: 0},
    {label: '迟到次数', value: counts.lateCount || 0, icon: 'Timer', unit: '', trend: 0}
  ]
  return userStore.user.role === 'STUDENT' ? student : common
})

// 2. 快捷入口配置 (颜色适配新风格)
const quickEntries = computed(() => {
  const role = userStore.user.role
  const entries = []
  if (role === 'STUDENT') {
    entries.push(
        {label: '签到打卡', icon: 'Position', path: '/attendance/checkin', bgColor: '#E6F4FF', iconColor: '#0694A2'},
        {label: '签到记录', icon: 'Memo', path: '/attendance/list', bgColor: '#FFF4E6', iconColor: '#FF9F43'},
        {label: '请假申请', icon: 'CoffeeCup', path: '/leave/my', bgColor: '#FEF9C3', iconColor: '#D97706'},
        {label: '我的课表', icon: 'Calendar', path: '/schedule/my', bgColor: '#F3F4F6', iconColor: '#64748B'}
    )
  } else {
    entries.push(
        {label: '发布签到', icon: 'Promotion', path: '/attendance/publish', bgColor: '#E6F4FF', iconColor: '#0694A2'},
        {label: '请假审批', icon: 'Stamp', path: '/leave/audit', bgColor: '#FFF4E6', iconColor: '#FF9F43'},
        {label: '考勤报表', icon: 'DataAnalysis', path: '/attendance/list', bgColor: '#FEF9C3', iconColor: '#D97706'},
        {
          label: '用户管理',
          icon: 'Setting',
          path: userStore.user.role === 'ADMIN' ? '/user/manage' : '/schedule/my',
          bgColor: '#F3F4F6',
          iconColor: '#64748B'
        }
    )
  }
  return entries
})

// 数据加载逻辑 (保持原有逻辑不变)
const initData = async () => {
  try {
    if (userStore.user.role === 'STUDENT') {
      const aiRes = await request.get('/ai/analysis', {params: {userId: userStore.user.id, role: userStore.user.role}})
      counts.checkInCount = aiRes.stats?.total || 0
      counts.attendanceRate = aiRes.stats?.attendanceRate || 0
      counts.normalCount = aiRes.stats?.normal || 0
      counts.lateCount = aiRes.stats?.late || 0
      // 学生角色暂时不显示课程排名图表
      // const rankRes = await getCourseRankForStudent(userStore.user.id)
      // initChart(rankRes)
    } else {
      const res = await getStatsCounts()
      const aiRes = await request.get('/ai/analysis', {params: {userId: userStore.user.id, role: userStore.user.role}})
      Object.assign(counts, res)
      counts.attendanceRate = aiRes.stats?.attendanceRate || 0
      if (userStore.user.role === 'TEACHER') counts.checkInCount = aiRes.stats?.total || 0
      const rankRes = await getCourseRank()
      initChart(rankRes)
    }
  } catch (e) {
    console.error(e)
  }

  if (userStore.user.role === 'STUDENT') {
    try {
      const sessionRes = await getActiveSessions({
        className: userStore.user.className || '计科1班',
        studentId: userStore.user.id
      })
      activeSessions.value = sessionRes || []
      // eslint-disable-next-line no-empty
    } catch (e) {
    }
  }

  // 获取今天的课程安排
  // eslint-disable-next-line
  await loadTodaySchedule()
  
  // 获取系统通知
  // eslint-disable-next-line
  await loadNotifications()
}

// 加载今日课程安排
const loadTodaySchedule = async () => {
  try {
    // 获取我的课表数据
    const res = await request.get('/schedule/my', { params: { userId: userStore.user.id } });
    const allSchedules = res || [];
    
    // 获取今天的星期数 (1-7 对应周一到周日)
    const today = new Date().getDay();
    const normalizedToday = today === 0 ? 7 : today;
    
    // 筛选今天的课程
    const todaySchedules = allSchedules.filter(course => course.weekDay === normalizedToday);
    
    // 按照时间段排序 (section字段表示第几大节)
    todaySchedules.sort((a, b) => a.section - b.section);
    
    // 格式化时间字符串
    const timeSlots = ['08:00-09:40', '10:00-11:40', '14:00-15:40', '16:00-17:40'];
    
    scheduleEvents.value = todaySchedules.map(course => ({
      time: timeSlots[course.section - 1] || `第${course.section}大节`,
      title: course.courseName,
      description: `${course.location} • ${course.className}`,
      type: 'course'
    }));
  } catch (error) {
    console.error('获取今日课程安排失败:', error);
  }
}

// 加载系统通知
const loadNotifications = async () => {
  try {
    // 目前后端没有通知接口，使用默认通知
    // 在实际应用中，这里可以从后端API获取真实通知
    notifications.value = [
      {
        time: '通知',
        title: '系统更新维护',
        description: '今晚凌晨2点进行系统维护',
        type: 'notification'
      }
    ];
  } catch (error) {
    console.error('获取系统通知失败:', error);
    // 出错时提供默认通知
    notifications.value = [
      {
        time: '通知',
        title: '系统更新维护',
        description: '今晚凌晨2点进行系统维护',
        type: 'notification'
      }
    ];
  }
}

// 刷新日程与通知
const refreshScheduleAndNotifications = async () => {
  await loadTodaySchedule();
  await loadNotifications();
}

// 刷新AI分析
const refreshAnalysis = async () => {
  aiLoading.value = true
  try {
    const params = {userId: userStore.user.id, role: userStore.user.role}
    if (userStore.user.role === 'STUDENT') params.className = userStore.user.className
    const res = await request.get('/ai/analysis', {params})
    Object.assign(aiAnalysis, res)
  } catch (e) {
    aiAnalysis.summary = 'AI系统正在根据您近期的活动分析您的表现...'
    aiAnalysis.trend = '正常'
  } finally {
    aiLoading.value = false
    initPieChart()
  }
}

// ECharts 初始化 (配色适配新风格)
const initChart = (dataList) => {
  nextTick(() => {
    const chartDom = document.getElementById('barChart')
    if (!chartDom) return
    if (echarts.getInstanceByDom(chartDom)) echarts.getInstanceByDom(chartDom).dispose()
    const myChart = echarts.init(chartDom)
    const xData = dataList.map(item => item.courseName || item.course_name)
    const yData = dataList.map(item => item.count)

    myChart.setOption({
      grid: {left: '0', right: '0', bottom: '0', top: '10px', containLabel: true},
      xAxis: {
        type: 'category',
        data: xData,
        axisLine: {show: false},
        axisTick: {show: false},
        axisLabel: {color: '#94A3B8'}
      },
      yAxis: {type: 'value', splitLine: {lineStyle: {type: 'dashed', color: '#F1F5F9'}}},
      tooltip: {trigger: 'axis', backgroundColor: '#fff', borderColor: '#F1F5F9', textStyle: {color: '#334155'}},
      series: [{
        type: 'bar', data: yData, barWidth: '16px',
        itemStyle: {borderRadius: [8, 8, 8, 8], color: '#0694A2'},
        showBackground: true, backgroundStyle: {color: '#F8FAFC', borderRadius: 8}
      }]
    })
    window.addEventListener('resize', () => myChart.resize())
  })
}

const initPieChart = () => {
  nextTick(() => {
    const chartDom = document.getElementById('pieChart')
    if (!chartDom) return
    if (echarts.getInstanceByDom(chartDom)) echarts.getInstanceByDom(chartDom).dispose()
    const myChart = echarts.init(chartDom)
    myChart.setOption({
      tooltip: {trigger: 'item'},
      legend: {bottom: '0', icon: 'circle', itemWidth: 8, itemHeight: 8},
      series: [{
        type: 'pie', radius: ['50%', '70%'], center: ['50%', '45%'],
        itemStyle: {borderRadius: 5, borderColor: '#fff', borderWidth: 2},
        label: {show: false},
        data: [
          {value: aiAnalysis.stats?.normal || 0, name: '正常', itemStyle: {color: '#0694A2'}},
          {value: aiAnalysis.stats?.late || 0, name: '迟到', itemStyle: {color: '#FFD166'}},
          {value: aiAnalysis.stats?.leave || 0, name: '请假', itemStyle: {color: '#FF9F43'}},
          {value: aiAnalysis.stats?.absent || 0, name: '缺勤', itemStyle: {color: '#FF6B6B'}}
        ]
      }]
    })
    window.addEventListener('resize', () => myChart.resize())
  })
}

onMounted(() => {
  initData()
  refreshAnalysis()
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

.weather {
  margin-left: 10px;
  color: #64748B;
}

/* 悬浮通知胶囊 */
.active-capsule {
  display: flex;
  align-items: center;
  background: #FFFFFF;
  padding: 6px 8px 6px 16px;
  border-radius: 30px;
  box-shadow: 0 4px 15px rgba(6, 148, 162, 0.15);
  border: 1px solid #E6F4FF;
}

.capsule-icon {
  color: #0694A2;
  margin-right: 10px;
  animation: spin 2s linear infinite;
}

.capsule-info {
  display: flex;
  gap: 8px;
  font-size: 13px;
  margin-right: 15px;
  align-items: center;
}

.capsule-info .label {
  color: #94A3B8;
  font-weight: 600;
  text-transform: uppercase;
  font-size: 11px;
}

.capsule-info .value {
  font-weight: 700;
  color: #334155;
}

.capsule-info .code {
  background: #E6F4FF;
  color: #0694A2;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
}

@keyframes spin {
  100% {
    transform: rotate(360deg);
  }
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

/* 3. 统计卡片 (我的数据面板) */
.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #1E293B;
  margin-bottom: 16px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.project-card {
  background: #FFFFFF;
  border-radius: 20px;
  padding: 20px;
  position: relative;
  overflow: hidden;
  height: 140px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
  transition: transform 0.3s;
}

.project-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.08);
}

/* 主题色定义 */
.theme-0 .icon-box {
  background: #E6F4FF;
  color: #0694A2;
}

.theme-1 .icon-box {
  background: #FFF4E6;
  color: #FF9F43;
}

.theme-2 .icon-box {
  background: #FEF9C3;
  color: #D97706;
}

.theme-3 .icon-box {
  background: #F3F4F6;
  color: #64748B;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: start;
  z-index: 2;
}

.icon-box {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.trend-badge {
  font-size: 12px;
  font-weight: 600;
  color: #10B981;
  display: flex;
  align-items: center;
  background: #ECFDF5;
  padding: 2px 6px;
  border-radius: 6px;
}

.card-content {
  z-index: 2;
}

.card-label {
  font-size: 13px;
  color: #94A3B8;
  margin-bottom: 4px;
}

.card-value {
  font-size: 28px;
  font-weight: 700;
  color: #334155;
}

.unit {
  font-size: 12px;
  font-weight: 500;
  margin-left: 2px;
  color: #94A3B8;
}

.card-wave {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 60px;
  opacity: 0.5;
  z-index: 1;
}

/* 4. AI 分析卡片 */
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

.ai-header {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.ai-avatar {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: linear-gradient(135deg, #0694A2, #3ad4e0);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  box-shadow: 0 4px 15px rgba(6, 148, 162, 0.3);
}

.ai-bubble {
  background: #F8FAFC;
  padding: 12px 16px;
  border-radius: 0 16px 16px 16px;
  font-size: 14px;
  color: #475569;
  line-height: 1.5;
  border: 1px solid #F1F5F9;
  flex: 1;
}

.ai-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
  margin-bottom: 15px;
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

.ai-risk-bar {
  background: #FFF7ED;
  color: #C2410C;
  padding: 10px 15px;
  border-radius: 12px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

/* 5. 图表区域 */
.charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.chart-container {
  display: flex;
  flex-direction: column;
}

.card-title {
  font-size: 15px;
  font-weight: 700;
  color: #1E293B;
  margin-bottom: 15px;
}

/* 6. 右侧栏 (工具与日程) */
.side-widget {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 30px;
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

/* 日程时间轴 */
.timeline-box {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.timeline-item {
  display: flex;
  gap: 15px;
  position: relative;
  padding-bottom: 24px;
}

.timeline-item::after {
  content: '';
  position: absolute;
  left: 63px;
  top: 30px;
  bottom: 0;
  width: 2px;
  background: #F1F5F9;
}

.timeline-item:last-child::after {
  display: none;
}

.tl-left {
  width: 45px;
  text-align: right;
  font-size: 12px;
  font-weight: 600;
  color: #94A3B8;
  padding-top: 4px;
}

.tl-content {
  flex: 1;
  padding: 12px 16px;
  border-radius: 12px;
  position: relative;
}

.tl-content::before {
  content: '';
  position: absolute;
  left: -22px;
  top: 8px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px #E2E8F0;
  z-index: 2;
  background: #cbd5e1;
}

.blue-bg {
  background: #E6F4FF;
}

.blue-bg::before {
  background: #0694A2;
  box-shadow: 0 0 0 2px #E6F4FF;
}

.orange-bg {
  background: #FFF4E6;
}

.orange-bg::before {
  background: #FF9F43;
  box-shadow: 0 0 0 2px #FFF4E6;
}

.gray-bg {
  background: #F8FAFC;
}

.tl-content h4 {
  margin: 0 0 4px;
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.tl-content p {
  margin: 0;
  font-size: 12px;
  color: #64748B;
}

/* 响应式适配 */
@media (max-width: 1200px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .charts-row {
    grid-template-columns: 1fr;
  }
}
</style>