<template>
  <div class="studio-dashboard">

    <!-- 顶部：欢迎栏 (极简风格) -->
    <div class="welcome-header">
      <div class="header-left">
        <h1>发起签到</h1>
        <p class="subtitle">创建新的课堂签到任务，支持多班级同时进行</p>
      </div>
      <div class="header-right">
        <div class="project-card" :class="'theme-0'" style="width: 180px; height: 50px;">
          <div class="card-content">
            <div class="card-label">进行中</div>
            <div class="card-value">{{ runningList.length }}<span class="unit"></span></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 核心布局：Grid System -->
    <div class="dashboard-grid">

      <!-- 左侧：主要内容区 (占 2/3) -->
      <div class="main-column">

        <!-- 发布面板 -->
        <div class="studio-card">
          <div class="card-header-row">
            <div class="section-title">新建签到任务</div>
          </div>
          <div class="card-header">
            <span class="card-title"><el-icon><Promotion /></el-icon> 新建签到任务</span>
            <el-tag effect="plain" type="info">快速发布</el-tag>
          </div>

          <div class="publish-form">
            <div class="form-row">
              <div class="form-item">
                <label>课程名称</label>
                <el-select v-model="form.courseName" placeholder="选择或输入课程" filterable allow-create class="form-select">
                  <el-option v-for="c in dynamicCourseList" :key="c" :label="c" :value="c" />
                </el-select>
              </div>
              <div class="form-item">
                <label>目标班级</label>
                <el-select v-model="form.targetClass" placeholder="选择班级" class="form-select" v-if="classList.length > 0">
                  <el-option v-for="c in classList" :key="c.id" :label="c.className" :value="c.className" />
                </el-select>
                <el-select v-else placeholder="班级加载中..." class="form-select" disabled />
              </div>
              <div class="form-item">
                <label>有效时长</label>
                <div class="duration-btns">
                  <button 
                    class="duration-btn" 
                    :class="{ active: form.duration === 5 }" 
                    @click="form.duration = 5"
                  >5 分钟</button>
                  <button 
                    class="duration-btn" 
                    :class="{ active: form.duration === 10 }" 
                    @click="form.duration = 10"
                  >10 分钟</button>
                </div>
              </div>
            </div>

            <div class="form-row">
              <div class="form-item location-switch">
                <div class="switch-wrapper">
                  <el-switch v-model="form.useLocation" />
                  <span class="switch-label">{{ form.useLocation ? '已开启位置限制' : '未开启位置限制' }}</span>
                </div>
                <p class="switch-tip" v-if="form.useLocation">学生需在指定范围内才能签到</p>
              </div>
              <div class="form-item publish-action">
                <el-button type="primary" size="large" @click="handlePublish" :loading="loading" class="publish-btn">
                  <el-icon><Promotion /></el-icon> 发布签到
                </el-button>
              </div>
            </div>
          </div>
          
          <!-- 位置状态提示 -->
          <transition name="el-fade-in">
            <div v-if="locationStatus" class="location-status">
              <el-alert :title="locationStatus" :type="locationStatusType" :closable="false" show-icon />
            </div>
          </transition>
        </div>

        <!-- 正在进行的任务列表 -->
        <div class="studio-card">
          <div class="card-header-row">
            <div class="section-title">正在进行中的签到</div>
          </div>
          <div class="task-list">
            <transition-group name="task-fade">
              <div v-for="task in runningList" :key="task.id" class="task-card">
                <!-- 雷达动画 -->
                <div class="radar-box">
                  <div class="radar-wave"></div>
                  <div class="radar-wave delay"></div>
                  <el-icon class="radar-icon"><Timer /></el-icon>
                </div>

                <!-- 任务信息 -->
                <div class="task-info">
                  <div class="task-header">
                    <h3 class="task-title">{{ task.courseName }}</h3>
                    <el-tag size="small" effect="light">{{ task.targetClass || '全校' }}</el-tag>
                  </div>
                  <div class="task-code">
                    <span class="code-label">口令</span>
                    <span class="code-value">{{ task.checkCode }}</span>
                  </div>
                  <div class="task-meta">
                    <span class="meta-item">
                      <el-icon><Clock /></el-icon>
                      失效: {{ formatTime(task.expireTime) }}
                    </span>
                    <el-tag v-if="task.latitude" size="small" type="warning" effect="plain">
                      <el-icon><Location /></el-icon> 位置限制
                    </el-tag>
                  </div>
                </div>

                <!-- 操作按钮 -->
                <div class="task-actions">
                  <el-tooltip content="大屏展示" placement="top">
                    <button class="action-btn qr-btn" @click="openQr(task)">
                      <el-icon><FullScreen /></el-icon>
                    </button>
                  </el-tooltip>
                  <el-tooltip content="结束签到" placement="top">
                    <button class="action-btn stop-btn" @click="handleStop(task)">
                      <el-icon><SwitchButton /></el-icon>
                    </button>
                  </el-tooltip>
                </div>
              </div>
            </transition-group>

            <div v-if="runningList.length === 0" class="empty-state">
              <el-empty description="当前没有正在进行的签到" :image-size="120" />
            </div>
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
            <div class="tool-item" @click="handlePublish">
              <div class="tool-icon" style="background: #E6F4FF; color: #0694A2">
                <el-icon><Promotion /></el-icon>
              </div>
              <span class="tool-name">发布签到</span>
            </div>
            <div class="tool-item" @click="fetchRunningTasks">
              <div class="tool-icon" style="background: #F3F4F6; color: #64748B">
                <el-icon><Refresh /></el-icon>
              </div>
              <span class="tool-name">刷新任务</span>
            </div>
          </div>
        </div>

        <!-- 签到统计信息 -->
        <div class="studio-card side-widget">
          <div class="widget-header">
            <span>签到统计</span>
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="ai-grid">
            <div class="ai-stat-box">
              <span class="label">进行中</span>
              <span class="val primary">{{ runningList.length }}</span>
            </div>
            <div class="ai-stat-box">
              <span class="label">已选择课程</span>
              <span class="val warning">{{ form.courseName || '—' }}</span>
            </div>
            <div class="ai-stat-box">
              <span class="label">位置限制</span>
              <span class="val danger">{{ form.useLocation ? '开启' : '关闭' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 二维码弹窗 -->
    <QrCodeModal v-model="showQrModal" :text="currentQrCode" />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, watch } from 'vue'
import { useUserStore } from '../../store/user'
import { publishCheckIn, stopCheckIn } from '../../api/attendance'
import request from '../../utils/request'
import { formatDate } from '../../utils/date'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import QrCodeModal from '../../components/QrCodeModal.vue'

const userStore = useUserStore()
const loading = ref(false)
const classList = ref([])
const dynamicCourseList = ref([])
const runningList = ref([]) // ★ 存储正在进行的任务列表

// 弹窗控制
const showQrModal = ref(false)
const currentQrCode = ref('')

// 位置状态
const locationStatus = ref('')
const locationStatusType = ref('info')

const form = reactive({ courseName: '', targetClass: '', duration: 10, useLocation: false })

// 监听位置限制开关变化
watch(() => form.useLocation, (newVal) => {
  if (newVal && !locationStatus.value) {
    locationStatus.value = '点击发布时将获取您的当前位置作为签到基准点'
    locationStatusType.value = 'info'
  } else if (!newVal) {
    locationStatus.value = ''
  }
})

// 初始化：加载字典 + 加载正在进行的任务
const init = async () => {
  try {
    // 1. 字典
    const classes = await request.get('/class/list', { hideError: true })
    classList.value = classes || []
    if (classList.value.length > 0) form.targetClass = classList.value[0].className

    const courses = await request.get('/schedule/courses', { hideError: true })
    dynamicCourseList.value = courses || []

    // 2. ★★★ 加载我正在进行的签到 ★★★
    fetchRunningTasks()

    // 3. 智能填充 (代码略，保持之前的 checkCurrentSchedule 逻辑即可)
    // eslint-disable-next-line no-empty
  } catch(e) {}
}

// 专门拉取进行中的任务
const fetchRunningTasks = async () => {
  const res = await request.get('/attendance/publishing', { params: { teacherId: userStore.user.id } })
  runningList.value = res || []
}

// 获取当前位置
const getLocation = () => {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject('浏览器不支持定位')
    } else {
      // 首先尝试高精度定位
      const highAccuracyOptions = {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 60000
      };
      
      navigator.geolocation.getCurrentPosition(
        pos => {
          const loc = { lat: pos.coords.latitude, lon: pos.coords.longitude }
          resolve(loc)
        },
        err => {
          // 如果是精度错误(code 3)，尝试使用低精度定位
          if (err.code === 3) {
            console.warn('高精度定位失败，尝试使用普通精度定位');
            
            const lowAccuracyOptions = {
              enableHighAccuracy: false, // 使用普通精度
              timeout: 15000, // 增加超时时间
              maximumAge: 120000 // 增加缓存时间
            };
            
            // 重新尝试获取位置
            navigator.geolocation.getCurrentPosition(
              pos => {
                const loc = { lat: pos.coords.latitude, lon: pos.coords.longitude }
                console.log('使用普通精度定位成功');
                resolve(loc)
              },
              err2 => {
                let msg = '定位失败'
                if(err2.code === 1) msg = '请允许浏览器获取位置权限'
                else if(err2.code === 2) msg = '获取位置信息超时，请重试'
                else if(err2.code === 3) msg = '获取位置信息过程中出现精度错误'
                else msg = '无法获取位置信息'
                
                if(window.location.protocol !== 'https:' && window.location.hostname !== 'localhost') {
                  msg += ' (非HTTPS环境可能影响定位功能)'
                }
                reject(msg)
              },
              lowAccuracyOptions
            );
          } else {
            // 其他错误直接返回
            let msg = '定位失败'
            if(err.code === 1) msg = '请允许浏览器获取位置权限'
            else if(err.code === 2) msg = '获取位置信息超时，请重试'
            else if(err.code === 3) msg = '获取位置信息过程中出现精度错误'
            else msg = '无法获取位置信息'
            
            if(window.location.protocol !== 'https:' && window.location.hostname !== 'localhost') {
              msg += ' (非HTTPS环境可能影响定位功能)'
            }
            reject(msg)
          }
        },
        highAccuracyOptions
      )
    }
  })
}

const handlePublish = async () => {
  if (!form.courseName) return ElMessage.warning('请选择课程')
  loading.value = true

  try {
    let locationData = { lat: null, lon: null }
    
    if (form.useLocation) {
      try {
        locationStatus.value = '正在获取您的位置...'
        locationStatusType.value = 'info'
        
        const loc = await getLocation()
        locationData = { lat: loc.lat, lon: loc.lon }
        locationStatus.value = `位置获取成功: ${loc.lat.toFixed(6)}, ${loc.lon.toFixed(6)}`
        locationStatusType.value = 'success'
        ElMessage.success('已获取当前位置')
      } catch (e) {
        const errorMsg = e || '获取位置失败，无法开启位置限制'
        locationStatus.value = errorMsg
        locationStatusType.value = 'error'
        ElMessage.error(errorMsg)
        loading.value = false
        return
      }
    } else {
      locationStatus.value = ''
    }

    await publishCheckIn({
      teacherId: userStore.user.id,
      courseName: form.courseName,
      targetClass: form.targetClass,
      lat: locationData.lat,
      lon: locationData.lon
    })
    ElMessage.success('发布成功')
    fetchRunningTasks() // ★ 发布后刷新列表
    // eslint-disable-next-line no-empty
  } catch(e) {} finally { loading.value = false }
}

const handleStop = (task) => {
  ElMessageBox.confirm(`确定结束【${task.courseName}】的签到吗？`, '提示', { type: 'warning' }).then(async () => {
    const res = await stopCheckIn({ sessionId: task.id })
    // 显示缺勤结果
    if (res.absentCount > 0) {
      ElMessageBox.alert(`缺勤：${res.absentCount}人\n名单：${res.absentNames.join(', ')}`, '结算报告')
    } else {
      ElMessage.success('结算完成')
    }
    fetchRunningTasks() // ★ 结束后刷新列表，该任务会消失
  })
}

const openQr = (task) => {
  currentQrCode.value = task.checkCode
  showQrModal.value = true
}

const formatTime = (t) => formatDate(t).split(' ')[1]

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

/* 发布卡片 */
.publish-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
  margin-bottom: 24px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 发布表单 */
.publish-form { display: flex; flex-direction: column; gap: 20px; }
.form-row { display: flex; gap: 24px; align-items: flex-end; flex-wrap: wrap; }
.form-item { display: flex; flex-direction: column; gap: 8px; }
.form-item label {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
}
.form-select { width: 200px; }
.form-select :deep(.el-input__wrapper) { border-radius: 10px; }
.duration-btns { display: flex; gap: 8px; }
.duration-btn {
  padding: 10px 20px;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  background: #fff;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}
.duration-btn:hover { border-color: #667eea; color: #667eea; }
.duration-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-color: transparent;
}
.location-switch { flex: 1; }
.switch-wrapper { display: flex; align-items: center; gap: 12px; }
.switch-label { font-size: 14px; color: #606266; }
.switch-tip { margin: 8px 0 0; font-size: 12px; color: #909399; }
.publish-action { margin-left: auto; }
.publish-btn {
  height: 44px;
  padding: 0 32px;
  border-radius: 10px;
  font-size: 15px;
}
.location-status { margin-top: 16px; }

/* 进行中区域 */
.running-section { margin-top: 8px; }
.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
}
.dot-pulse {
  width: 10px;
  height: 10px;
  background: #f56c6c;
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.2); }
}

/* 任务卡片 */
.task-list { display: flex; flex-direction: column; gap: 16px; }
.task-card {
  display: flex;
  align-items: center;
  padding: 24px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
  border-left: 4px solid #667eea;
  transition: all 0.3s;
}
.task-card:hover {
  transform: translateX(4px);
  box-shadow: 0 8px 30px rgba(0,0,0,0.1);
}

/* 雷达动画 */
.radar-box {
  position: relative;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24px;
}
.radar-icon { font-size: 28px; color: #667eea; z-index: 2; }
.radar-wave {
  position: absolute;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  opacity: 0.2;
  animation: radar 2s infinite ease-out;
}
.radar-wave.delay { animation-delay: 0.5s; }
@keyframes radar {
  0% { transform: scale(0.5); opacity: 0.6; }
  100% { transform: scale(2); opacity: 0; }
}

/* 任务信息 */
.task-info { flex: 1; }
.task-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.task-title { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.task-code {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, #fff7e6 0%, #fff2d9 100%);
  padding: 8px 16px;
  border-radius: 8px;
  margin-bottom: 12px;
}
.code-label { font-size: 13px; color: #e6a23c; }
.code-value {
  font-family: 'Monaco', monospace;
  font-size: 20px;
  font-weight: 700;
  color: #e6a23c;
  letter-spacing: 2px;
}
.task-meta { display: flex; align-items: center; gap: 16px; font-size: 13px; color: #909399; }
.meta-item { display: flex; align-items: center; gap: 4px; }

/* 操作按钮 */
.task-actions { display: flex; gap: 12px; }
.action-btn {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.3s;
}
.qr-btn {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: #fff;
}
.qr-btn:hover { transform: scale(1.1); }
.stop-btn {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
  color: #fff;
}
.stop-btn:hover { transform: scale(1.1); }

/* 空状态 */
.empty-state {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
}

/* 动画 */
.task-fade-enter-active,
.task-fade-leave-active {
  transition: all 0.4s ease;
}
.task-fade-enter-from,
.task-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
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