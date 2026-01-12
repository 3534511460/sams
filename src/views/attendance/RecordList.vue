<template>
  <div class="studio-dashboard">

    <!-- 顶部：欢迎栏 (极简风格) -->
    <div class="welcome-header">
      <div class="header-left">
        <h1>考勤记录</h1>
        <p class="subtitle">查看和管理所有考勤打卡记录</p>
      </div>
      <div class="header-right">
        <div class="project-card" :class="'theme-0'" style="width: 180px; height: 50px;">
          <div class="card-content">
            <div class="card-label">总记录</div>
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
              <div class="filter-item" v-if="userRole !== 'STUDENT'">
                <label>学生姓名</label>
                <el-input v-model="queryParams.studentName" placeholder="输入姓名" clearable />
              </div>
              <div class="filter-item">
                <label>课程名称</label>
                <el-input v-model="queryParams.courseName" placeholder="输入课程" clearable />
              </div>
              <div class="filter-item" v-if="['ADMIN', 'STUDENT'].includes(userRole)">
                <label>教师姓名</label>
                <el-input v-model="queryParams.teacherName" placeholder="输入教师" clearable />
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
              <el-button type="success" @click="handleExport"><el-icon><Download /></el-icon> 导出</el-button>
              <el-button v-if="userRole !== 'STUDENT'" type="warning" :disabled="selectedIds.length === 0" @click="handleBatchEdit">
                <el-icon><Edit /></el-icon> 批量修改 ({{ selectedIds.length }})
              </el-button>
            </div>
          </div>
        </div>

        <!-- 数据表格 -->
        <div class="studio-card">
          <div class="card-header-row">
            <div class="section-title">考勤记录列表</div>
          </div>
          <el-table
              :data="tableData"
              v-loading="loading"
              :header-cell-style="{ background: '#fafafa', color: '#606266', fontWeight: '600', height: '52px' }"
              :row-style="{ height: '60px' }"
              @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="50" align="center" v-if="userRole !== 'STUDENT'" />
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column prop="courseName" label="课程名称" min-width="140" />
            <el-table-column prop="teacherName" label="发起老师" width="100" />
            <el-table-column prop="studentName" label="学生姓名" width="100" />
            <el-table-column label="打卡时间" width="170">
              <template #default="{ row }">{{ formatTime(row.checkInTime) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.status===1" type="success" effect="dark" round>正常</el-tag>
                <el-tag v-else-if="row.status===2" type="warning" effect="dark" round>迟到</el-tag>
                <el-tag v-else-if="row.status===4" color="#626aef" effect="dark" style="border:none" round>请假</el-tag>
                <el-tag v-else type="danger" effect="dark" round>缺勤</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="申诉情况" width="120" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.appealStatus===0" type="info" size="small" effect="plain">未申诉</el-tag>
                <el-tooltip v-else :content="row.appealReason" placement="top">
                  <el-tag v-if="row.appealStatus===1" type="warning">审核中</el-tag>
                  <el-tag v-else-if="row.appealStatus===2" type="success">已通过</el-tag>
                  <el-tag v-else type="danger">已驳回</el-tag>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" align="center" fixed="right">
              <template #default="{ row }">
                <el-button v-if="userRole==='STUDENT' && row.status===3 && row.appealStatus===0" type="primary" size="small" round @click="handleAppeal(row)">申诉</el-button>
                <template v-if="userRole!=='STUDENT' && row.appealStatus===1">
                  <el-button type="success" size="small" round @click="handleAudit(row,2)">通过</el-button>
                  <el-button type="danger" size="small" round @click="handleAudit(row,3)">驳回</el-button>
                </template>
                <el-button v-if="userRole!=='STUDENT' && row.appealStatus!==1" type="primary" size="small" round @click="handleEdit(row)">修改</el-button>
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
            <div class="tool-item" @click="handleSearch">
              <div class="tool-icon" style="background: #E6F4FF; color: #0694A2">
                <el-icon><Search /></el-icon>
              </div>
              <span class="tool-name">查询记录</span>
            </div>
            <div class="tool-item" @click="resetQuery">
              <div class="tool-icon" style="background: #F3F4F6; color: #64748B">
                <el-icon><Refresh /></el-icon>
              </div>
              <span class="tool-name">重置筛选</span>
            </div>
            <div class="tool-item" @click="handleExport">
              <div class="tool-icon" style="background: #FEF9C3; color: #D97706">
                <el-icon><Download /></el-icon>
              </div>
              <span class="tool-name">导出记录</span>
            </div>
          </div>
        </div>

        <!-- 考勤统计信息 -->
        <div class="studio-card side-widget">
          <div class="widget-header">
            <span>考勤统计</span>
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
            <div class="ai-stat-box">
              <span class="label">已选记录</span>
              <span class="val danger">{{ selectedIds.length }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 弹窗：申诉 -->
    <el-dialog v-model="appealVisible" title="申诉理由" width="420px" align-center>
      <el-input v-model="appealReason" type="textarea" :rows="4" placeholder="请详细说明申诉理由..." />
      <template #footer>
        <el-button @click="appealVisible=false">取消</el-button>
        <el-button type="primary" @click="submitAppeal">提交申诉</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗：修改 -->
    <el-dialog v-model="editVisible" :title="isBatch ? '批量修改状态' : '修改考勤状态'" width="420px" align-center>
      <el-form label-width="80px">
        <el-form-item label="对象" v-if="!isBatch">
          <el-input v-model="editForm.studentName" disabled />
        </el-form-item>
        <el-form-item label="已选" v-else>
          <el-tag type="warning" size="large">{{ selectedIds.length }} 条记录</el-tag>
        </el-form-item>
        <el-form-item label="更改为">
          <el-radio-group v-model="editForm.status">
            <el-radio :label="1">正常</el-radio>
            <el-radio :label="2">迟到</el-radio>
            <el-radio :label="3">缺勤</el-radio>
            <el-radio :label="4">请假</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible=false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// 完整导入Vue核心API（包含watch监听）
import { onMounted, reactive, ref, computed, watch } from 'vue'
// 导入考勤相关接口
import { getRecordList, updateRecord, appealRecord, auditAppeal, batchUpdateRecord } from '../../api/attendance'
// 导入用户状态管理
import { useUserStore } from '../../store/user'
// 导入日期格式化工具
import { formatDate } from '../../utils/date'
// 导入请求工具
import request from '../../utils/request'
// 导入Element Plus组件
import { ElMessage, ElMessageBox } from 'element-plus'
// 导入路由钩子（包含useRoute和useRouter）
import { useRoute, useRouter } from 'vue-router'

// 获取路由实例和路由跳转实例
const route = useRoute()
const router = useRouter()
// 获取用户Store实例
const userStore = useUserStore()
// 计算属性：获取当前用户角色
const userRole = computed(() => userStore.user.role)

// 核心状态变量定义
const loading = ref(false) // 表格加载状态
const tableData = ref([]) // 表格数据
const total = ref(0) // 总记录数
const pageNum = ref(1) // 当前页码
const pageSize = ref(10) // 每页条数
const dateRange = ref([]) // 日期范围选择
const queryParams = reactive({ // 查询参数
  studentName: '',
  courseName: '',
  teacherName: ''
})

// 多选相关变量
const selectedIds = ref([]) // 选中的记录ID集合
const isBatch = ref(false) // 标记是否为批量操作

// 专门提取检查并弹出申诉处理弹窗的方法
const checkAndPopup = () => {
  const targetId = route.query.highlightId
  if (!targetId) return

  // 查找目标考勤记录
  const targetRow = tableData.value.find(item => item.id == targetId)

  // 仅对“申诉中(1)”状态的记录弹窗
  if (targetRow && targetRow.appealStatus === 1) {
    ElMessageBox.confirm(
        `学生【${targetRow.studentName}】发起了申诉：\n"${targetRow.appealReason}"\n\n是否同意修改为正常状态？`,
        '申诉处理',
        {
          confirmButtonText: '同意申诉',
          cancelButtonText: '驳回申诉',
          distinguishCancelAndClose: true,
          type: 'info'
        }
    ).then(() => {
      handleAudit(targetRow, 2) // 同意申诉（pass=2）
    }).catch((action) => {
      if (action === 'cancel') {
        handleAudit(targetRow, 3) // 驳回申诉（pass=3）
      }
    })

    // 清除路由中的highlightId参数，避免重复弹窗
    router.replace({ path: route.path, query: {} })
  }
}

// 异步获取考勤记录数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getRecordList({
      ...queryParams,
      userId: userStore.user.id,
      role: userStore.user.role,
      startTime: dateRange.value ? dateRange.value[0] : null,
      endTime: dateRange.value ? dateRange.value[1] : null,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    tableData.value = res.records || []
    total.value = res.total || 0

    // 数据加载完成后检查是否需要弹出申诉处理弹窗
    checkAndPopup()
  } catch(e) {
    tableData.value = [] // 异常时清空表格数据
    ElMessage.error('数据加载失败') // 新增异常提示，提升用户体验
  } finally {
    loading.value = false // 无论成功失败，都关闭加载状态
  }
}

// 格式化时间显示（为空时显示短横线）
const formatTime = (val) => val ? formatDate(val) : '-'

// 分页相关事件处理
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchData()
}
const handlePageChange = (val) => {
  pageNum.value = val
  fetchData()
}

// 搜索和重置事件处理
const handleSearch = () => {
  pageNum.value = 1 // 搜索时重置页码为第一页
  fetchData()
}
const resetQuery = () => {
  // 清空所有查询参数
  queryParams.studentName = ''
  queryParams.courseName = ''
  queryParams.teacherName = ''
  dateRange.value = []
  handleSearch() // 重置后重新查询
}

// 导出考勤记录为Excel
const handleExport = () => {
  request({
    url: '/attendance/export',
    method: 'get',
    responseType: 'blob', // 关键：指定响应类型为blob
    params: {
      ...queryParams,
      userId: userStore.user.id,
      role: userStore.user.role,
      startTime: dateRange.value?.[0],
      endTime: dateRange.value?.[1]
    }
  }).then(res => {
    // 创建下载链接并触发下载
    const url = window.URL.createObjectURL(new Blob([res]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', '考勤记录.xlsx')
    document.body.appendChild(link)
    link.click()
    // 释放URL对象，避免内存泄漏
    window.URL.revokeObjectURL(url)
    document.body.removeChild(link)
  }).catch(() => {
    ElMessage.error('导出失败')
  })
}

// 申诉相关状态和方法
const appealVisible = ref(false) // 申诉弹窗显示状态
const appealReason = ref('') // 申诉理由
const currentRecordId = ref(null) // 当前申诉的记录ID

// 打开申诉弹窗
const handleAppeal = (row) => {
  currentRecordId.value = row.id
  appealReason.value = '' // 清空原有申诉理由
  appealVisible.value = true
}

// 提交申诉
const submitAppeal = async () => {
  if (!appealReason.value.trim()) {
    ElMessage.warning('请输入申诉理由')
    return
  }
  try {
    await appealRecord({
      recordId: currentRecordId.value,
      reason: appealReason.value
    })
    ElMessage.success('提交成功')
    appealVisible.value = false
    fetchData() // 提交后刷新数据
  } catch (e) {
    ElMessage.error('申诉提交失败')
  }
}

// 申诉审核处理
const handleAudit = async (row, pass) => {
  try {
    await auditAppeal({
      recordId: row.id,
      pass
    })
    ElMessage.success('操作成功')
    fetchData() // 审核后刷新数据
  } catch (e) {
    ElMessage.error('审核操作失败')
  }
}

// 编辑相关状态和方法
const editVisible = ref(false) // 编辑弹窗显示状态
const editForm = reactive({ // 编辑表单数据
  id: null,
  studentName: '',
  status: 1
})

// 表格多选状态变化处理
const handleSelectionChange = (val) => {
  selectedIds.value = val.map(item => item.id)
}

// 打开单个记录编辑弹窗
const handleEdit = (row) => {
  isBatch.value = false
  editForm.id = row.id
  editForm.studentName = row.studentName
  editForm.status = row.status
  editVisible.value = true
}

// 打开批量修改弹窗
const handleBatchEdit = () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择需要修改的记录')
    return
  }
  isBatch.value = true
  editForm.status = 1 // 默认选中“正常”状态
  editVisible.value = true
}

// 提交编辑（单个/批量通用）
const submitEdit = async () => {
  try {
    if (isBatch.value) {
      // 批量修改逻辑
      await batchUpdateRecord({
        ids: selectedIds.value,
        status: editForm.status
      })
      ElMessage.success('批量修改成功')
    } else {
      // 单个修改逻辑
      await updateRecord({
        id: editForm.id,
        status: editForm.status,
        operatorId: userStore.user.id,
        operatorRole: userStore.user.role
      })
      ElMessage.success('修改成功')
    }
    editVisible.value = false
    fetchData() // 编辑后刷新数据
  } catch (e) {
    ElMessage.error(isBatch.value ? '批量修改失败' : '修改失败')
  }
}

// 核心修复：监听路由query中的highlightId变化，触发数据刷新
watch(() => route.query.highlightId, (newVal) => {
  if (newVal) {
    fetchData()
  }
})

// 组件挂载时初始化加载数据
onMounted(() => {
  fetchData()
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

/* 筛选卡片 */
.filter-form { display: flex; flex-direction: column; gap: 16px; }
.filter-row { display: flex; gap: 20px; flex-wrap: wrap; align-items: flex-end; }
.filter-item { display: flex; flex-direction: column; gap: 8px; }
.filter-item label { font-size: 13px; font-weight: 500; color: #606266; }
.filter-item :deep(.el-input) { width: 160px; }
.filter-item :deep(.el-input__wrapper) { border-radius: 8px; }
.filter-actions { display: flex; gap: 12px; }

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