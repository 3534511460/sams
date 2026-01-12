<template>
  <div class="studio-dashboard">

    <!-- 顶部：欢迎栏 (极简风格) -->
    <div class="welcome-header">
      <div class="header-left">
        <h1>学生签到</h1>
        <p class="subtitle">输入口令完成课堂签到</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="init" class="add-btn">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <!-- 核心布局：Grid System -->
    <div class="dashboard-grid">

      <!-- 左侧：主要内容区 (占 2/3) -->
      <div class="main-column">

        <!-- 有任务时显示 -->
        <transition name="el-zoom-in-center">
          <div v-if="activeTask" class="task-active-card">
            <div class="task-pulse">
              <div class="pulse-ring"></div>
              <div class="pulse-ring delay"></div>
              <el-icon class="pulse-icon"><Bell /></el-icon>
            </div>
            <div class="task-info">
              <h2 class="task-course">{{ activeTask.courseName }}</h2>
              <div class="task-location" v-if="activeTask.latitude">
                <el-icon><LocationInformation /></el-icon>
                <span v-if="myDistance !== null">距离老师: <strong>{{ myDistance }}</strong> 米</span>
                <span v-else>正在定位中...</span>
              </div>
              <div class="task-location" v-else>
                <el-icon><Unlock /></el-icon>
                <span>本次签到无位置限制</span>
              </div>
            </div>
          </div>
        </transition>

        <!-- 无任务时显示 -->
        <div v-if="!activeTask && !loading" class="no-task-card">
          <div class="no-task-icon">
            <el-icon><Coffee /></el-icon>
          </div>
          <h3>当前暂无考勤任务</h3>
          <p>请留意老师发布的签到通知</p>
        </div>

        <!-- 签到操作卡片 -->
        <div v-if="activeTask" class="studio-card">
          <div class="card-header-row">
            <div class="section-title">签到操作</div>
          </div>
          <div class="card-header">
            <el-icon><Key /></el-icon>
            <span>输入 6 位签到口令</span>
          </div>
          
          <div class="code-input-wrapper">
            <el-input 
              v-model="code" 
              placeholder="• • • • • •" 
              maxlength="6"
              class="code-input"
              size="large"
              type="tel"
            />
          </div>

          <el-button 
            type="primary" 
            class="submit-btn" 
            size="large" 
            @click="handleSubmit" 
            :loading="submitting" 
            :disabled="isOutOfRange"
          >
            <el-icon v-if="!submitting"><Check /></el-icon>
            {{ isOutOfRange ? '距离过远，无法签到' : '立即签到' }}
          </el-button>
          
          <div class="location-debug" v-if="myLoc">
            <el-icon><Location /></el-icon>
            我的坐标: {{ myLoc.lat.toFixed(4) }}, {{ myLoc.lon.toFixed(4) }}
          </div>
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
            <div class="tool-item" @click="init">
              <div class="tool-icon" style="background: #E6F4FF; color: #0694A2">
                <el-icon><Refresh /></el-icon>
              </div>
              <span class="tool-name">刷新任务</span>
            </div>
            <div class="tool-item" @click="code = ''">
              <div class="tool-icon" style="background: #F3F4F6; color: #64748B">
                <el-icon><Delete /></el-icon>
              </div>
              <span class="tool-name">清空口令</span>
            </div>
          </div>
        </div>

        <!-- 考勤统计信息 -->
        <div class="studio-card side-widget">
          <div class="widget-header">
            <span>考勤状态</span>
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="ai-grid">
            <div class="ai-stat-box">
              <span class="label">距离老师</span>
              <span class="val primary">{{ myDistance !== null ? myDistance + 'm' : '—' }}</span>
            </div>
            <div class="ai-stat-box">
              <span class="label">签到状态</span>
              <span class="val warning">{{ activeTask ? '进行中' : '待开始' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import { studentCheckIn, getActiveSessions } from '../../api/attendance'
import { useUserStore } from '../../store/user'
import { ElMessage, ElLoading } from 'element-plus'
import { Loading, LocationInformation } from '@element-plus/icons-vue'

const userStore = useUserStore()
const code = ref('')
const submitting = ref(false)
const loading = ref(false)
const activeTask = ref(null) 
const myLoc = ref(null)     // 我的位置
const myDistance = ref(null) // 距离

// 计算是否超出范围 (100米)
const isOutOfRange = computed(() => {
  // 如果老师没设位置，或者还没算出距离，就不限制
  if (!activeTask.value?.latitude || myDistance.value === null) return false
  return myDistance.value > 100
})

// 地球距离计算公式 (Haversine Formula)
const calcDistance = (lat1, lon1, lat2, lon2) => {
  const R = 6378.137 // 地球半径 km
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLon = (lon2 - lon1) * Math.PI / 180
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLon/2) * Math.sin(dLon/2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
  const d = R * c
  return Math.round(d * 1000) // 返回米
}

// 获取位置
const getLocation = () => {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject('浏览器不支持定位')
    } else {
      // 首先尝试高精度定位
      const highAccuracyOptions = {
        enableHighAccuracy: true,
        timeout: 5000,
        maximumAge: 60000
      };
      
      navigator.geolocation.getCurrentPosition(
        pos => {
          const loc = { lat: pos.coords.latitude, lon: pos.coords.longitude }
          myLoc.value = loc
          resolve(loc)
        },
        err => {
          // 如果是精度错误(code 3)，尝试使用低精度定位
          if (err.code === 3) {
            console.warn('高精度定位失败，尝试使用普通精度定位');
            
            const lowAccuracyOptions = {
              enableHighAccuracy: false, // 使用普通精度
              timeout: 10000, // 增加超时时间
              maximumAge: 120000 // 增加缓存时间
            };
            
            // 重新尝试获取位置
            navigator.geolocation.getCurrentPosition(
              pos => {
                const loc = { lat: pos.coords.latitude, lon: pos.coords.longitude }
                myLoc.value = loc
                console.log('使用普通精度定位成功');
                resolve(loc)
              },
              err2 => {
                // 这里处理 localhost 能用但 http://ip 不能用的问题
                let msg = '定位失败'
                if(err2.code === 1) msg = '请允许浏览器获取位置权限'
                else if(err2.code === 2) msg = '获取位置信息超时，请重试'
                else if(err2.code === 3) msg = '获取位置信息过程中出现精度错误'
                else if(window.location.protocol !== 'https:' && window.location.hostname !== 'localhost') {
                  msg = '当前非HTTPS环境，浏览器禁止定位 (安全限制)'
                }
                else msg = '无法获取位置信息'
                reject(msg)
              },
              lowAccuracyOptions
            );
          } else {
            // 其他错误直接返回
            let msg = '定位失败'
            if(err.code === 1) msg = '请允许浏览器获取位置权限'
            else if(err.code === 2) msg = '获取位置信息超时，请重试'
            else if(window.location.protocol !== 'https:' && window.location.hostname !== 'localhost') {
              msg = '当前非HTTPS环境，浏览器禁止定位 (安全限制)'
            }
            else msg = '无法获取位置信息'
            reject(msg)
          }
        },
        highAccuracyOptions
      )
    }
  })
}

const init = async () => {
  loading.value = true
  try {
    const myClass = userStore.user.className || '计科1班'
    const tasks = await getActiveSessions({ className: myClass, studentId: userStore.user.id })
    
    if (tasks && tasks.length > 0) {
      activeTask.value = tasks[0]
      
      //  如果任务有坐标，立即启动定位并计算距离
      if (activeTask.value.latitude) {
        try {
          const loc = await getLocation()
          // 计算距离
          const dist = calcDistance(
            activeTask.value.latitude, activeTask.value.longitude,
            loc.lat, loc.lon
          )
          myDistance.value = dist
        } catch (e) {
          ElMessage.error(e) // 弹出定位失败的具体原因
        }
      }
    }
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (code.value.length !== 6) return ElMessage.warning('请输入6位口令')
  
  // 再次校验位置 (防止进入页面后跑远了)
  if (activeTask.value?.latitude && !myLoc.value) {
    return ElMessage.error('必须获取位置才能签到！')
  }

  submitting.value = true
  try {
    await studentCheckIn({
      studentId: userStore.user.id,
      code: code.value,
      // 必须把坐标传给后端做二次校验
      lat: myLoc.value ? myLoc.value.lat : null,
      lon: myLoc.value ? myLoc.value.lon : null
    })
    ElMessage.success(' 签到成功！')
    code.value = ''
    activeTask.value = null 
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}

onMounted(() => init())
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
  grid-template-columns: repeat(3, 1fr);
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
  color: #909399;
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

/* 有任务时的卡片 */
.task-active-card {
  background: #fff;
  border-radius: 20px;
  padding: 30px;
  box-shadow: 0 10px 40px rgba(102, 126, 234, 0.2);
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 24px;
}
.task-pulse {
  position: relative;
  width: 70px;
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.pulse-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  animation: pulse-wave 2s infinite;
}
.pulse-ring.delay { animation-delay: 0.5s; }
@keyframes pulse-wave {
  0% { transform: scale(0.5); opacity: 0.8; }
  100% { transform: scale(1.5); opacity: 0; }
}
.pulse-icon {
  font-size: 32px;
  color: #667eea;
  z-index: 2;
  background: #fff;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.task-info { flex: 1; }
.task-course {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 10px 0;
}
.task-location {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #e6a23c;
}
.task-location strong { font-size: 18px; }

/* 无任务卡片 */
.no-task-card {
  background: #fff;
  border-radius: 20px;
  padding: 60px 30px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.08);
  text-align: center;
}
.no-task-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8e8e8 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  color: #909399;
}
.no-task-card h3 {
  font-size: 18px;
  color: #303133;
  margin: 0 0 8px 0;
}
.no-task-card p {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

/* 签到操作卡片 */
.card-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 16px;
  color: #606266;
  margin-bottom: 24px;
}
.code-input-wrapper { margin-bottom: 24px; }
.code-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  padding: 8px 16px;
}
.code-input :deep(.el-input__inner) {
  text-align: center;
  font-size: 32px;
  letter-spacing: 12px;
  font-weight: 700;
  color: #303133;
}
.submit-btn {
  width: 100%;
  height: 52px;
  font-size: 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}
.submit-btn:hover {
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
}
.submit-btn:disabled {
  background: #e4e7ed;
  color: #909399;
}
.location-debug {
  margin-top: 16px;
  text-align: center;
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
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