<template>
  <div class="studio-dashboard">

    <!-- 顶部：欢迎栏 (极简风格) -->
    <div class="welcome-header">
      <div class="header-left">
        <h1>请假审批</h1>
        <p class="subtitle">审核学生请假申请，管理今日休假名单</p>
      </div>
      <div class="header-right">
        <div class="stats-row">
          <div class="project-card" :class="'theme-0'">
            <div class="card-content">
              <div class="card-label">待审批</div>
              <div class="card-value">{{ pendingCount }}<span class="unit"></span></div>
            </div>
          </div>
          <div class="project-card" :class="'theme-1'">
            <div class="card-content">
              <div class="card-label">今日休假</div>
              <div class="card-value">{{ todayList.length }}<span class="unit"></span></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 核心布局：Grid System -->
    <div class="dashboard-grid">

      <!-- 左侧：主要内容区 (占 2/3) -->
      <div class="main-column">

        <!-- 今日休假看板 -->
        <div class="studio-card">
          <div class="card-header-row">
            <div class="section-title">今日休假中</div>
            <el-button :icon="Refresh" circle size="small" @click="fetchToday" />
          </div>

          <div v-if="todayList.length > 0" class="leave-grid">
            <div v-for="item in todayList" :key="item.id" class="leave-card">
              <div class="leave-header">
                <div class="leave-avatar" :style="{ background: getAvatarColor(item.studentName) }">
                  {{ item.studentName ? item.studentName.charAt(0) : 'S' }}
                </div>
                <div class="leave-info">
                  <span class="leave-name">{{ item.studentName }}</span>
                  <span class="leave-class">{{ item.className }}</span>
                </div>
                <el-tag size="small" :type="item.type === '病假' ? 'danger' : 'warning'" effect="dark">
                  {{ item.type }}
                </el-tag>
              </div>
              <div class="leave-reason">
                <el-icon><Document /></el-icon>
                {{ item.reason }}
              </div>
            </div>
          </div>

          <div v-else class="empty-today">
            <el-icon class="check-icon"><CircleCheckFilled /></el-icon>
            <span>今日无人请假，全员在岗</span>
          </div>
        </div>

        <!-- 审批列表 -->
        <div class="studio-card">
          <div class="card-header-row">
            <div class="section-title">审批流程处理</div>
            <el-button :icon="Refresh" circle @click="refreshAll" />
          </div>

          <el-table 
            :data="tableData" 
            v-loading="loading" 
            :header-cell-style="{ background: '#fafafa', color: '#606266', fontWeight: '600' }"
            :row-style="{ height: '72px' }"
          >
            <el-table-column label="申请人" width="160">
              <template #default="{ row }">
                <div class="applicant-cell">
                  <div class="applicant-avatar" :style="{ background: getAvatarColor(row.studentName) }">
                    {{ row.studentName ? row.studentName.charAt(0) : 'S' }}
                  </div>
                  <span class="applicant-name">{{ row.studentName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="row.type==='病假'?'danger':'info'" effect="light" round>{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="请假理由" show-overflow-tooltip min-width="180" />
            <el-table-column label="时间范围" width="320">
              <template #default="{ row }">
                <div class="time-range">
                  <el-icon><Clock /></el-icon>
                  {{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="当前状态" width="140" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.status===0" type="warning" effect="dark">⏳ 待审批</el-tag>
                <el-tag v-else-if="row.status===1" type="success" effect="dark">✅ 已通过</el-tag>
                <el-tag v-else-if="row.status===2" type="danger" effect="dark">❌ 已驳回</el-tag>
                <el-tag v-else-if="row.status===3" color="#f56c6c" effect="dark" style="border:none">🚨 申请销假</el-tag>
                <el-tag v-else-if="row.status===4" type="info" effect="dark">🗑️ 已销假</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" align="center">
              <template #default="{ row }">
                <div v-if="row.status===0" class="action-btns">
                  <el-button type="success" size="small" round @click="handleAudit(row, 1)">
                    <el-icon><Check /></el-icon> 通过
                  </el-button>
                  <el-button type="danger" size="small" round @click="handleAudit(row, 2)">
                    <el-icon><Close /></el-icon> 驳回
                  </el-button>
                </div>
                <div v-else-if="row.status===3" class="action-btns">
                  <el-button type="warning" size="small" round @click="handleAudit(row, 1)">确认销假</el-button>
                  <el-button type="info" size="small" round @click="handleAudit(row, 2)">拒绝</el-button>
                </div>
                <span v-else class="processed-text">已处理</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-footer">
            <div class="pagination-info">
              共 <span class="total-num">{{ total }}</span> 条记录
            </div>
            <el-pagination
                v-model:current-page="pageNum"
                v-model:page-size="pageSize"
                background
                layout="prev, pager, next"
                :total="total"
                @current-change="handlePageChange" 
            />
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
            <div class="tool-item" @click="refreshAll">
              <div class="tool-icon" style="background: #E6F4FF; color: #0694A2">
                <el-icon><Refresh /></el-icon>
              </div>
              <span class="tool-name">刷新数据</span>
            </div>
            <div class="tool-item" @click="fetchToday">
              <div class="tool-icon" style="background: #F3F4F6; color: #64748B">
                <el-icon><Sunny /></el-icon>
              </div>
              <span class="tool-name">今日休假</span>
            </div>
          </div>
        </div>

        <!-- 请假统计信息 -->
        <div class="studio-card side-widget">
          <div class="widget-header">
            <span>请假统计</span>
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="ai-grid">
            <div class="ai-stat-box">
              <span class="label">待审批</span>
              <span class="val primary">{{ pendingCount }}</span>
            </div>
            <div class="ai-stat-box">
              <span class="label">今日休假</span>
              <span class="val warning">{{ todayList.length }}</span>
            </div>
            <div class="ai-stat-box">
              <span class="label">当前页数</span>
              <span class="val danger">{{ pageNum }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { onMounted, ref, watch, computed } from 'vue'
import { useUserStore } from '../../store/user'
import { useRoute, useRouter } from 'vue-router'
import { getLeaveList, auditLeave, getTodayLeaves } from '../../api/leave'
import { formatDate } from '../../utils/date'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Sunny, Refresh, CircleCheckFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const tableData = ref([])
const todayList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

// 待审批数量
const pendingCount = computed(() => tableData.value.filter(item => item.status === 0 || item.status === 3).length)

// 生成头像颜色
const getAvatarColor = (name) => {
  const colors = [
    'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)',
    'linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%)',
    'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    'linear-gradient(135deg, #fa709a 0%, #fee140 100%)'
  ]
  if (!name) return colors[0]
  return colors[name.charCodeAt(0) % colors.length]
}

// 专门提取检查并弹出快捷处理弹窗的方法
const checkAndPopup = () => {
  const targetId = route.query.highlightId
  if (!targetId) return

  // 在当前审批列表中查找目标请假记录
  const targetRow = tableData.value.find(item => item.id == targetId)

  // 仅对“待审批(0)”或“申请销假(3)”状态的记录弹窗处理
  if (targetRow && (targetRow.status === 0 || targetRow.status === 3)) {
    const actionText = targetRow.status === 3 ? '销假' : '请假'

    ElMessageBox.confirm(
        `检测到针对学生【${targetRow.studentName}】的${actionText}请求，是否立即处理？`,
        '快捷处理',
        {
          confirmButtonText: '同意/通过',
          cancelButtonText: '拒绝/驳回',
          distinguishCancelAndClose: true,
          type: 'warning'
        }
    ).then(() => {
      handleAudit(targetRow, 1) // 同意/通过操作
    }).catch((action) => {
      // 仅当点击“取消”按钮时执行驳回逻辑
      if (action === 'cancel') {
        handleAudit(targetRow, 2) // 拒绝/驳回操作
      }
    })

    // 清除路由中的highlightId参数，防止刷新页面重复弹窗
    router.replace({ path: route.path, query: {} })
  }
}

// 异步获取请假审批列表数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getLeaveList({
      userId: userStore.user.id,
      role: userStore.user.role,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    tableData.value = res.records || []
    total.value = res.total || 0

    // 数据加载完成后检查是否需要弹出快捷处理弹窗
    checkAndPopup()
  } catch (e) {
    tableData.value = []
    ElMessage.error('审批列表加载失败')
  } finally {
    loading.value = false
  }
}

// 异步获取今日休假名单
const fetchToday = async () => {
  try {
    const res = await getTodayLeaves()
    todayList.value = res || []
  } catch (e) {
    todayList.value = []
    ElMessage.error('今日休假数据加载失败')
  }
}

// 刷新所有数据（审批列表 + 今日休假名单）
const refreshAll = () => {
  fetchData()
  fetchToday()
}

// 格式化时间显示（为空时显示空字符串）
const formatTime = (val) => val ? formatDate(val) : ''

// 请假/销假审批处理
const handleAudit = async (row, status) => {
  try {
    await auditLeave({ id: row.id, status })
    ElMessage.success('操作成功')
    refreshAll() // 操作后刷新所有数据
  } catch (e) {
    ElMessage.error('审批操作失败')
  }
}

// 分页页码变化处理
const handlePageChange = (val) => {
  pageNum.value = val
  fetchData()
}

// 核心修复：监听路由query中的highlightId变化，触发数据刷新
watch(() => route.query.highlightId, (newVal) => {
  if (newVal) {
    fetchData()
  }
})

// 组件挂载时初始化加载所有数据
onMounted(() => {
  refreshAll()
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
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.project-card {
  background: #FFFFFF;
  border-radius: 16px; /* 原20px，略小更紧凑 */
  padding: 12px 16px; /* 原20px，减少内边距 */
  position: relative;
  overflow: hidden;
  height: 80px; /* 原140px，大幅缩小高度 */
  display: flex;
  flex-direction: column;
  justify-content: center; /* 改为垂直居中，替代原space-between */
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
  font-size: 20px; /* 原28px，缩小字体 */
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

/* 今日看板 */
.today-board {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
}
.board-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.board-title {
  font-weight: 600;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #303133;
}

/* 休假卡片网格 */
.leave-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}
.leave-card {
  background: linear-gradient(135deg, #f5f7fa 0%, #fff 100%);
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.3s;
}
.leave-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
  border-color: #667eea;
}
.leave-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.leave-avatar {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
  font-size: 16px;
}
.leave-info { flex: 1; }
.leave-name { font-weight: 600; color: #303133; display: block; }
.leave-class { font-size: 12px; color: #909399; }
.leave-reason {
  font-size: 13px;
  color: #606266;
  background: #f9fafb;
  padding: 10px 12px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 空状态 */
.empty-today {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #67C23A;
  background: linear-gradient(135deg, #f0f9eb 0%, #e8f5e0 100%);
  padding: 20px;
  border-radius: 12px;
  font-size: 14px;
}
.check-icon { font-size: 24px; }

/* 审批卡片 */
.audit-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 表格样式 */
.applicant-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.applicant-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
  font-size: 14px;
}
.applicant-name { font-weight: 500; color: #303133; }
.time-range {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}
.action-btns { display: flex; gap: 8px; justify-content: center; }
.processed-text { color: #c0c4cc; font-size: 13px; }

/* 分页 */
.pagination-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}
.pagination-info { font-size: 13px; color: #909399; }
.total-num { color: #667eea; font-weight: 600; }

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