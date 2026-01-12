<template>
  <div class="studio-dashboard">

    <!-- 顶部：欢迎栏 (极简风格) -->
    <div class="welcome-header">
      <div class="header-left">
        <h1>班级管理</h1>
        <p class="subtitle">管理系统中的所有班级信息</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="openAddDialog" class="add-btn">
          <el-icon><Plus /></el-icon> 新增班级
        </el-button>
      </div>
    </div>

    <!-- 核心布局：Grid System -->
    <div class="dashboard-grid">

      <!-- 左侧：主要内容区 (占 2/3) -->
      <div class="main-column">

        <!-- 1. 我的数据面板 (统计数据 - 设计项目卡片风格) -->
        <div class="stats-container">
          <div class="section-title">班级统计</div>
          <div class="stats-row">
            <div class="project-card" :class="'theme-0'">
              <div class="card-top">
                <div class="icon-box"><el-icon><School /></el-icon></div>
              </div>
              <div class="card-content">
                <div class="card-label">班级总数</div>
                <div class="card-value">{{ tableData.length }}<span class="unit"></span></div>
              </div>
              <!-- 装饰性波纹 -->
              <svg class="card-wave" viewBox="0 0 1440 320" preserveAspectRatio="none">
                <path fill="rgba(255,255,255,0.3)" d="M0,224L48,213.3C96,203,192,181,288,181.3C384,181,480,203,576,224C672,245,768,267,864,250.7C960,235,1056,181,1152,165.3C1248,149,1344,171,1392,181.3L1440,192L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z"></path>
              </svg>
            </div>
            <div class="project-card" :class="'theme-1'">
              <div class="card-top">
                <div class="icon-box"><el-icon><UserFilled /></el-icon></div>
              </div>
              <div class="card-content">
                <div class="card-label">学生总数</div>
                <div class="card-value">{{ totalStudents }}<span class="unit"></span></div>
              </div>
              <!-- 装饰性波纹 -->
              <svg class="card-wave" viewBox="0 0 1440 320" preserveAspectRatio="none">
                <path fill="rgba(255,255,255,0.3)" d="M0,224L48,213.3C96,203,192,181,288,181.3C384,181,480,203,576,224C672,245,768,267,864,250.7C960,235,1056,181,1152,165.3C1248,149,1344,171,1392,181.3L1440,192L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z"></path>
              </svg>
            </div>
            <div class="project-card" :class="'theme-2'">
              <div class="card-top">
                <div class="icon-box"><el-icon><TrendCharts /></el-icon></div>
              </div>
              <div class="card-content">
                <div class="card-label">平均人数/班</div>
                <div class="card-value">{{ avgStudents }}<span class="unit"></span></div>
              </div>
              <!-- 装饰性波纹 -->
              <svg class="card-wave" viewBox="0 0 1440 320" preserveAspectRatio="none">
                <path fill="rgba(255,255,255,0.3)" d="M0,224L48,213.3C96,203,192,181,288,181.3C384,181,480,203,576,224C672,245,768,267,864,250.7C960,235,1056,181,1152,165.3C1248,149,1344,171,1392,181.3L1440,192L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z"></path>
              </svg>
            </div>
          </div>
        </div>

        <!-- 班级管理卡片 -->
        <div class="studio-card">
          <div class="card-header-row">
            <div class="section-title">班级列表</div>
            <div class="toolbar-right">
              <el-input
                  v-model="searchText"
                  placeholder="搜索班级名称..."
                  prefix-icon="Search"
                  class="search-input"
                  clearable
              />
              <el-button :icon="Refresh" @click="fetchData" circle />
            </div>
          </div>
          <div class="class-grid" v-loading="loading">
            <transition-group name="fade">
              <div
                  v-for="item in filteredData"
                  :key="item.id"
                  class="class-card"
              >
                <div class="class-card-header">
                  <div class="class-avatar" :style="{ background: getColor(item.className) }">
                    {{ item.className ? item.className.charAt(0) : 'C' }}
                  </div>
                  <div class="class-info">
                    <h3 class="class-name">{{ item.className }}</h3>
                    <span class="class-id">ID: {{ item.id }}</span>
                  </div>
                </div>
                <div class="class-card-body">
                  <div class="class-stat">
                    <el-icon><UserFilled /></el-icon>
                    <span>{{ item.studentCount || 0 }}</span> 名学生
                  </div>
                </div>
                <div class="class-card-footer">
                  <el-button type="primary" link @click="handleEdit(item)">
                    <el-icon><Edit /></el-icon> 编辑
                  </el-button>
                  <el-popconfirm
                      title="删除班级可能导致关联的学生数据异常，确定删除吗？"
                      @confirm="handleDelete(item)"
                      width="260"
                  >
                    <template #reference>
                      <el-button type="danger" link>
                        <el-icon><Delete /></el-icon> 删除
                      </el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </div>
            </transition-group>

            <!-- 空状态 -->
            <div v-if="filteredData.length === 0 && !loading" class="empty-state">
              <el-empty description="暂无班级数据">
                <el-button type="primary" @click="openAddDialog">立即添加</el-button>
              </el-empty>
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
            <div class="tool-item" @click="openAddDialog">
              <div class="tool-icon" style="background: #E6F4FF; color: #0694A2">
                <el-icon><Plus /></el-icon>
              </div>
              <span class="tool-name">新增班级</span>
            </div>
            <div class="tool-item" @click="fetchData">
              <div class="tool-icon" style="background: #F3F4F6; color: #64748B">
                <el-icon><Refresh /></el-icon>
              </div>
              <span class="tool-name">刷新数据</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
        v-model="dialogVisible"
        :title="isEdit ? '编辑班级' : '新增班级'"
        width="450px"
        class="custom-dialog"
        align-center
    >
      <div class="dialog-content">
        <div class="dialog-icon">
          <el-icon><School /></el-icon>
        </div>
        <el-form label-position="top" class="dialog-form">
          <el-form-item label="班级名称">
            <el-input
                v-model="formData.className"
                placeholder="例如：计算机科学与技术1班"
                size="large"
                @keyup.enter="submitForm"
            >
              <template #prefix>
                <el-icon><School /></el-icon>
              </template>
            </el-input>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false" size="large">取消</el-button>
        <el-button type="primary" @click="submitForm" :disabled="!formData.className" size="large">
          {{ isEdit ? '保存修改' : '确认添加' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref, computed, reactive } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const searchText = ref('')
const isEdit = ref(false)
const formData = reactive({
  id: null,
  className: ''
})

// 统计数据
const totalStudents = ref(0)
const avgStudents = computed(() => tableData.value.length ? Math.round(totalStudents.value / tableData.value.length) : 0)

// 获取统计数据
const fetchStats = async () => {
  try {
    const res = await request.get('/stats/counts')
    totalStudents.value = res.studentCount || 0
  } catch (e) {
    console.error('获取统计数据失败', e)
  }
}

// 过滤后的数据
const filteredData = computed(() => {
  if (!searchText.value) return tableData.value
  return tableData.value.filter(item =>
      item.className.toLowerCase().includes(searchText.value.toLowerCase())
  )
})

// 生成随机颜色
const getColor = (name) => {
  const colors = [
    'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)',
    'linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%)',
    'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)'
  ]
  if (!name) return colors[0]
  return colors[name.charCodeAt(0) % colors.length]
}

// 获取列表
const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/class/list')
    tableData.value = res || []
  } finally {
    loading.value = false
  }
}

// 打开新增弹窗
const openAddDialog = () => {
  isEdit.value = false
  formData.id = null
  formData.className = ''
  dialogVisible.value = true
}

// 打开编辑弹窗
const handleEdit = (row) => {
  isEdit.value = true
  formData.id = row.id
  formData.className = row.className
  dialogVisible.value = true
}

// 提交表单
const submitForm = async () => {
  if (!formData.className) return
  try {
    if (isEdit.value) {
      await request.post('/class/update', formData)
      ElMessage.success('修改成功')
    } else {
      await request.post('/class/add', { className: formData.className })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch(e) {
    // 错误处理
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await request.delete(`/class/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch(e) {
    // 错误处理
  }
}

onMounted(() => {
  fetchData()
  fetchStats()
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

/* 班级卡片网格 */
.class-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  min-height: 200px;
}

.class-card {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #ebeef5;
  padding: 20px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.class-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(0,0,0,0.1);
  border-color: transparent;
}

.class-card-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.class-avatar {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 600;
  color: #fff;
}

.class-info {
  flex: 1;
  overflow: hidden;
}

.class-name {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.class-id {
  font-size: 12px;
  color: #909399;
}

.class-card-body {
  padding: 12px 0;
  border-top: 1px dashed #ebeef5;
  border-bottom: 1px dashed #ebeef5;
}

.class-stat {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.class-stat .el-icon {
  color: #909399;
}

.class-card-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
}

/* 空状态 */
.empty-state {
  grid-column: 1 / -1;
  padding: 40px 0;
}

/* 弹窗样式 */
.dialog-content {
  text-align: center;
}

.dialog-icon {
  width: 70px;
  height: 70px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: #fff;
}

.dialog-form {
  text-align: left;
}

.dialog-form :deep(.el-input__wrapper) {
  border-radius: 10px;
}

/* 搜索输入框 */
.search-input {
  width: 280px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e4e7ed inset;
}

/* 工具栏右侧 */
.toolbar-right {
  display: flex;
  gap: 10px;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(20px);
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