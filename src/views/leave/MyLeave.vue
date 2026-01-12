<template>
  <div class="studio-dashboard">

    <!-- 顶部：欢迎栏 (极简风格) -->
    <div class="welcome-header">
      <div class="header-left">
        <h1>我的请假</h1>
        <p class="subtitle">查看请假记录和申请新的请假</p>
      </div>
      <div class="header-right">
        <el-button type="primary" size="large" @click="dialogVisible = true">
          <el-icon><Plus /></el-icon> 申请请假
        </el-button>
      </div>
    </div>

    <!-- 核心布局：Grid System -->
    <div class="dashboard-grid">

      <!-- 左侧：主要内容区 (占 2/3) -->
      <div class="main-column">

        <!-- 搜索筛选区 -->
        <div class="studio-card">
          <div class="section-title">筛选条件</div>
          <div class="filter-form">
            <div class="filter-row">
              <div class="filter-item">
                <label>请假类型</label>
                <el-select v-model="queryParams.type" placeholder="选择类型" clearable style="width: 160px;">
                  <el-option label="病假" value="病假" />
                  <el-option label="事假" value="事假" />
                </el-select>
              </div>
              <div class="filter-item">
                <label>状态</label>
                <el-select v-model="queryParams.status" placeholder="选择状态" clearable style="width: 160px;">
                  <el-option label="待审批" :value="0" />
                  <el-option label="已通过" :value="1" />
                  <el-option label="已驳回" :value="2" />
                  <el-option label="销假中" :value="3" />
                  <el-option label="已销假" :value="4" />
                </el-select>
              </div>
              <div class="filter-item">
                <label>时间范围</label>
                <el-date-picker
                    v-model="dateRange"
                    type="datetimerange"
                    range-separator="至"
                    start-placeholder="开始"
                    end-placeholder="结束"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    style="width: 340px"
                />
              </div>
            </div>
            <div class="filter-actions">
              <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 查询</el-button>
              <el-button @click="resetQuery"><el-icon><Refresh /></el-icon> 重置</el-button>
            </div>
          </div>
        </div>

        <!-- 请假列表 -->
        <div class="studio-card">
          <div class="card-header-row">
            <div class="section-title">请假记录</div>
          </div>
          <el-table :data="tableData" v-loading="loading" :header-cell-style="{ background: '#fafafa', color: '#606266', fontWeight: '600', height: '52px' }" :row-style="{ height: '56px' }">
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="row.type === '病假' ? 'danger' : 'info'" effect="dark" round>{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="时间范围" width="320">
              <template #default="{ row }">
                <div class="time-range">
                  <el-icon><Clock /></el-icon>
                  {{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="请假理由" min-width="200" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.status === 0" type="warning" effect="dark" round>⏳ 待审批</el-tag>
                <el-tag v-else-if="row.status === 1" type="success" effect="dark" round>✅ 已通过</el-tag>
                <el-tag v-else-if="row.status === 2" type="danger" effect="dark" round>❌ 已驳回</el-tag>
                <el-tag v-else-if="row.status === 3" color="#e6a23c" effect="dark" style="border:none" round>🔄 销假中</el-tag>
                <el-tag v-else-if="row.status === 4" type="info" effect="dark" round>🗑️ 已销假</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" align="center" fixed="right">
              <template #default="{ row }">
                <el-popconfirm title="确定要申请销假吗？" @confirm="handleCancel(row)">
                  <template #reference>
                    <el-button v-if="row.status === 1" type="warning" size="small" round>申请销假</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-footer">
            <div class="pagination-info">共 <span class="total-num">{{ total }}</span> 条记录</div>
            <el-pagination
                v-model:current-page="pageNum"
                v-model:page-size="pageSize"
                :page-sizes="[10, 20, 50]"
                background
                layout="sizes, prev, pager, next"
                :total="total"
                @size-change="handleSizeChange"
                @current-change="handlePageChange" />
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
            <div class="tool-item" @click="dialogVisible = true">
              <div class="tool-icon" style="background: #E6F4FF; color: #0694A2">
                <el-icon><Plus /></el-icon>
              </div>
              <span class="tool-name">申请请假</span>
            </div>
            <div class="tool-item" @click="fetchData">
              <div class="tool-icon" style="background: #F3F4F6; color: #64748B">
                <el-icon><Refresh /></el-icon>
              </div>
              <span class="tool-name">刷新数据</span>
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
              <span class="label">总记录数</span>
              <span class="val primary">{{ total }}</span>
            </div>
            <div class="ai-stat-box">
              <span class="label">当前页数</span>
              <span class="val warning">{{ pageNum }}</span>
            </div>
            <div class="ai-stat-box">
              <span class="label">每页数量</span>
              <span class="val danger">{{ pageSize }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 申请弹窗 -->
    <el-dialog v-model="dialogVisible" title="填写请假申请" width="500px" align-center>
      <el-form :model="form" label-width="80px">
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio label="病假">病假</el-radio>
            <el-radio label="事假">事假</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker v-model="dateRange" type="datetimerange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="理由">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请详细说明请假理由..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useUserStore } from '../../store/user'
import { applyLeave, getLeaveList, cancelLeaveApply } from '../../api/leave'
import { formatDate } from '../../utils/date'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Plus, Clock, Grid, DataAnalysis } from '@element-plus/icons-vue'

const userStore = useUserStore()
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const dialogVisible = ref(false)
const dateRange = ref([])
const form = reactive({ type: '事假', reason: '' })

// 筛选参数
const queryParams = reactive({
  type: '',
  status: ''
})

const formatTime = (val) => val ? formatDate(val) : ''

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getLeaveList({
      userId: userStore.user.id,
      role: userStore.user.role,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      type: queryParams.type,
      status: queryParams.status,
      startTime: dateRange.value?.[0],
      endTime: dateRange.value?.[1]
    })
    tableData.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

const handleSubmit = async () => {
  if (!dateRange.value || dateRange.value.length < 2) return ElMessage.warning('请选择时间')
  await applyLeave({
    studentId: userStore.user.id, type: form.type, reason: form.reason,
    startTime: dateRange.value[0], endTime: dateRange.value[1]
  })
  ElMessage.success('提交成功')
  dialogVisible.value = false
  fetchData()
}

// 销假逻辑
const handleCancel = async (row) => {
  await cancelLeaveApply({ id: row.id })
  ElMessage.success('销假申请已提交')
  fetchData()
}

const handlePageChange = (val) => { 
  pageNum.value = val; 
  fetchData() 
}

const handleSizeChange = (val) => {
  pageSize.value = val
  fetchData()
}

const handleSearch = () => {
  pageNum.value = 1
  fetchData()
}

const resetQuery = () => {
  queryParams.type = ''
  queryParams.status = ''
  dateRange.value = []
  handleSearch()
}

onMounted(() => fetchData())
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
  letter-spacing: 0.5px;
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

/* 筛选卡片 */
.filter-form { display: flex; flex-direction: column; gap: 16px; }
.filter-row { display: flex; gap: 20px; flex-wrap: wrap; align-items: flex-end; }
.filter-item { display: flex; flex-direction: column; gap: 8px; }
.filter-item label { font-size: 13px; font-weight: 500; color: #606266; }
.filter-item :deep(.el-input) { width: 160px; }
.filter-item :deep(.el-input__wrapper) { border-radius: 8px; }
.filter-actions { display: flex; gap: 12px; }

/* 时间范围 */
.time-range {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

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