<template>
  <div class="studio-dashboard">

    <!-- 顶部：欢迎栏 (极简风格) -->
    <div class="welcome-header">
      <div class="header-left">
        <h1>系统操作日志</h1>
        <p class="subtitle">记录系统所有操作行为，便于安全审计与问题追溯</p>
      </div>
      <div class="header-right">
        <div class="project-card" :class="'theme-0'" style="width: 180px; height: 50px;">
          <div class="card-content">
            <div class="card-label">日志总数</div>
            <div class="card-value">{{ total }}<span class="unit"></span></div>
          </div>
        </div>
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
                <label>操作人</label>
                <el-input v-model="searchText" placeholder="输入用户名" clearable @clear="fetchData" prefix-icon="User" />
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

        <!-- 数据表格 -->
        <div class="studio-card">
          <div class="card-header-row">
            <div class="section-title">日志列表</div>
          </div>
          <el-table
              :data="tableData"
              v-loading="loading"
              :header-cell-style="{ background: '#fafafa', color: '#606266', fontWeight: '600', height: '52px' }"
              :row-style="{ height: '56px' }"
          >
            <el-table-column type="index" label="#" width="60" align="center" />
            <el-table-column prop="username" label="操作人" width="120">
              <template #default="{ row }">
                <div class="user-cell">
                  <el-avatar :size="28" class="mini-avatar">{{ row.username?.charAt(0) }}</el-avatar>
                  <span>{{ row.username }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="operation" label="操作内容" width="160">
              <template #default="{ row }">
                <el-tag :type="getOpTagType(row.operation)" effect="light" round>{{ row.operation || '常规请求' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="method" label="请求接口" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <code class="api-code">{{ row.method }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="ip" label="IP地址" width="140">
              <template #default="{ row }">
                <span class="ip-text">{{ row.ip }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="操作时间" width="180">
              <template #default="{ row }">
                <div class="time-cell">
                  <el-icon><Clock /></el-icon>
                  <span>{{ formatTime(row.createTime) }}</span>
                </div>
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
            <div class="tool-item" @click="handleSearch">
              <div class="tool-icon" style="background: #E6F4FF; color: #0694A2">
                <el-icon><Search /></el-icon>
              </div>
              <span class="tool-name">查询日志</span>
            </div>
            <div class="tool-item" @click="resetQuery">
              <div class="tool-icon" style="background: #F3F4F6; color: #64748B">
                <el-icon><Refresh /></el-icon>
              </div>
              <span class="tool-name">重置筛选</span>
            </div>
          </div>
        </div>

        <!-- 日志统计信息 -->
        <div class="studio-card side-widget">
          <div class="widget-header">
            <span>日志统计</span>
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="ai-grid">
            <div class="ai-stat-box">
              <span class="label">当前页数</span>
              <span class="val primary">{{ pageNum }}</span>
            </div>
            <div class="ai-stat-box">
              <span class="label">每页数量</span>
              <span class="val warning">{{ pageSize }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import request from '../../utils/request'
import { formatDate } from '../../utils/date'
import { Search, Refresh, Document, Clock } from '@element-plus/icons-vue'

const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const searchText = ref('')
const dateRange = ref([])
const loading = ref(false)

const formatTime = (val) => val ? formatDate(val) : '-'

// 根据操作类型返回标签颜色
const getOpTagType = (op) => {
  if (!op) return 'info'
  if (op.includes('删除')) return 'danger'
  if (op.includes('新增') || op.includes('添加')) return 'success'
  if (op.includes('修改') || op.includes('更新')) return 'warning'
  if (op.includes('登录')) return 'primary'
  return 'info'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/log/list', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        username: searchText.value,
        startTime: dateRange.value?.[0],
        endTime: dateRange.value?.[1]
      }
    })
    tableData.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  fetchData()
}

const resetQuery = () => {
  searchText.value = ''
  dateRange.value = []
  handleSearch()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  fetchData()
}

const handlePageChange = (val) => {
  pageNum.value = val
  fetchData()
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

/* 筛选卡片 */
.filter-form { display: flex; flex-direction: column; gap: 16px; }
.filter-row { display: flex; gap: 20px; flex-wrap: wrap; align-items: flex-end; }
.filter-item { display: flex; flex-direction: column; gap: 8px; }
.filter-item label { font-size: 13px; font-weight: 500; color: #606266; }
.filter-item :deep(.el-input) { width: 180px; }
.filter-item :deep(.el-input__wrapper) { border-radius: 8px; }
.filter-actions { display: flex; gap: 12px; }

/* 用户单元格 */
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mini-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
  font-size: 12px;
}

/* API代码样式 */
.api-code {
  background: #f5f7fa;
  padding: 4px 8px;
  border-radius: 4px;
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 12px;
  color: #606266;
}

/* IP文本 */
.ip-text {
  font-family: 'Monaco', 'Consolas', monospace;
  color: #909399;
}

/* 时间单元格 */
.time-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
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