<template>
  <div class="studio-dashboard">

    <!-- 顶部：欢迎栏 (极简风格) -->
    <div class="welcome-header">
      <div class="header-left">
        <h1>用户管理</h1>
        <p class="subtitle">管理系统中的学生和老师信息</p>
      </div>
      <div class="header-right">
        <el-upload
            action="#"
            :show-file-list="false"
            :http-request="handleUpload"
            accept=".xlsx, .xls"
        >
          <el-button class="header-btn" size="large">
            <el-icon><Upload /></el-icon> 导入用户
          </el-button>
        </el-upload>
        <el-button type="primary" size="large" @click="handleAdd(activeTab)">
          <el-icon><Plus /></el-icon> 新增{{ activeTab === 'STUDENT' ? '学生' : '老师' }}
        </el-button>
      </div>
    </div>

    <!-- 核心布局：Grid System -->
    <div class="dashboard-grid">

      <!-- 左侧：主要内容区 (占 2/3) -->
      <div class="main-column">

        <!-- 用户列表 -->
        <div class="studio-card">
          <div class="card-header-row">
            <div class="section-title">{{ activeTab === 'STUDENT' ? '学生' : '老师' }}列表</div>
            <div class="tab-buttons">
              <button 
                class="tab-btn" 
                :class="{ active: activeTab === 'STUDENT' }" 
                @click="switchTab('STUDENT')"
              >
                <el-icon><UserFilled /></el-icon> 学生管理
              </button>
              <button 
                class="tab-btn" 
                :class="{ active: activeTab === 'TEACHER' }" 
                @click="switchTab('TEACHER')"
              >
                <el-icon><Avatar /></el-icon> 老师管理
              </button>
            </div>
          </div>
          <!-- 搜索工具栏 -->
          <div class="toolbar">
            <el-input
                v-model="searchText"
                placeholder="搜索姓名或工号/学号..."
                prefix-icon="Search"
                class="search-input"
                @keyup.enter="fetchData"
                clearable
                @clear="fetchData"
            />
            <el-button :icon="Refresh" @click="fetchData" circle />
          </div>
          <el-table
              :data="tableData"
              style="width: 100%"
              :header-cell-style="{ background: '#fafafa', color: '#606266', height: '52px', fontWeight: '600' }"
              :row-style="{ height: '72px' }"
          >
            <!-- 头像列 -->
            <el-table-column width="80" align="center">
              <template #default="{ row }">
                <div class="avatar-wrapper">
                  <el-avatar :size="44" :style="{ background: generateColor(row.name) }">
                    {{ row.name ? row.name.charAt(0) : 'U' }}
                  </el-avatar>
                  <span class="online-dot"></span>
                </div>
              </template>
            </el-table-column>

            <!-- 基本信息列 -->
            <el-table-column label="基本信息" min-width="180">
              <template #default="{ row }">
                <div class="user-info">
                  <span class="user-name">{{ row.name }}</span>
                  <span class="user-id">
                    <el-icon><Ticket /></el-icon>
                    {{ activeTab === 'STUDENT' ? '学号' : '工号' }}: {{ row.username }}
                  </span>
                </div>
              </template>
            </el-table-column>

            <!-- 班级/部门列 -->
            <el-table-column
                :label="activeTab === 'STUDENT' ? '所属班级' : '负责部门'"
                min-width="160"
            >
              <template #default="{ row }">
                <el-tag 
                  v-if="row.className" 
                  :type="activeTab === 'STUDENT' ? 'primary' : 'success'"
                  effect="light"
                  class="class-tag"
                >
                  <el-icon><School /></el-icon>
                  {{ row.className }}
                </el-tag>
                <span v-else class="no-class">未分配</span>
              </template>
            </el-table-column>

            <!-- 操作列 -->
            <el-table-column label="操作" width="160" align="center">
              <template #default="{ row }">
                <el-button type="primary" link @click="handleEdit(row)">
                  <el-icon><Edit /></el-icon> 编辑
                </el-button>
                <el-popconfirm 
                  title="确定删除该用户吗？" 
                  @confirm="handleDelete(row)"
                  width="200"
                >
                  <template #reference>
                    <el-button type="danger" link>
                      <el-icon><Delete /></el-icon> 删除
                    </el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>

          <!-- 空状态 -->
          <div v-if="tableData.length === 0 && !loading" class="empty-state">
            <el-empty :description="`暂无${activeTab === 'STUDENT' ? '学生' : '老师'}数据`">
              <el-button type="primary" @click="handleAdd(activeTab)">立即添加</el-button>
            </el-empty>
          </div>

          <!-- 分页 -->
          <div class="pagination-footer">
            <div class="pagination-info">
              共 <span class="total-num">{{ total }}</span> 条记录
            </div>
            <el-pagination
                background
                layout="prev, pager, next"
                :total="total"
                :page-size="10"
                v-model:current-page="pageNum"
                @current-change="fetchData"
            />
          </div>
        </div>
      </div>

      <!-- 右侧：侧边栏 (占 1/3) -->
      <div class="right-col">
        <!-- 统计信息 -->
        <div class="studio-card side-widget">
          <div class="widget-header">
            <span>统计信息</span>
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="ai-grid">
            <div class="ai-stat-box" :class="{ active: activeTab === 'STUDENT' }" @click="switchTab('STUDENT')">
              <span class="label">学生总数</span>
              <span class="val primary">{{ statsData.studentCount }}</span>
            </div>
            <div class="ai-stat-box" :class="{ active: activeTab === 'TEACHER' }" @click="switchTab('TEACHER')">
              <span class="label">老师总数</span>
              <span class="val warning">{{ statsData.teacherCount }}</span>
            </div>
            <div class="ai-stat-box">
              <span class="label">班级数量</span>
              <span class="val success">{{ classList.length }}</span>
            </div>
            <div class="ai-stat-box">
              <span class="label">部门数量</span>
              <span class="val info">{{ departmentList.length }}</span>
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
            <div class="tool-item" @click="handleAdd(activeTab)">
              <div class="tool-icon" style="background: #E6F4FF; color: #0694A2">
                <el-icon><Plus /></el-icon>
              </div>
              <span class="tool-name">新增{{ activeTab === 'STUDENT' ? '学生' : '老师' }}</span>
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

    <!-- 编辑/新增弹窗：保留原有逻辑，微调样式 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑资料' : '新增用户'" width="450px" align-center>
      <el-form :model="form" label-position="top">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="账号/学号/工号">
              <el-input v-model="form.username" :disabled="!!form.id" placeholder="请输入账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名">
              <el-input v-model="form.name" placeholder="请输入真实姓名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item :label="activeTab === 'STUDENT' ? '所属班级' : '负责部门/教研室'">
          <el-select v-if="activeTab === 'STUDENT' && classList.length > 0" v-model="form.className" style="width: 100%" placeholder="请选择班级">
            <el-option v-for="c in classList" :key="c.id" :label="c.className" :value="c.className" />
          </el-select>
          <el-select v-else-if="activeTab === 'STUDENT'" v-model="form.className" style="width: 100%" placeholder="班级加载中..." disabled>
            <el-option value="" label="班级加载中..." />
          </el-select>
          <el-select v-else-if="departmentList.length > 0" v-model="form.className" style="width: 100%" placeholder="请选择部门/教研室">
            <el-option v-for="d in departmentList" :key="d.id" :label="d.departmentName" :value="d.departmentName" />
          </el-select>
          <el-select v-else v-model="form.className" style="width: 100%" placeholder="部门加载中..." disabled>
            <el-option value="" label="部门加载中..." />
          </el-select>
        </el-form-item>

        <el-form-item label="初始密码" v-if="!form.id">
          <el-input v-model="form.password" placeholder="默认为 123456" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import {onMounted, reactive, ref} from 'vue'
import request from '../../utils/request'
import {ElMessage, ElMessageBox} from 'element-plus'
import {
  Refresh,
  User,
  Upload,
  Plus,
  UserFilled,
  Avatar,
  School,
  OfficeBuilding,
  Ticket,
  Edit,
  Delete,
  Search,
  DataAnalysis,
  Grid
} from '@element-plus/icons-vue'

const activeTab = ref('STUDENT')
const tableData = ref([])
const classList = ref([])
const departmentList = ref([])
const total = ref(0)
const searchText = ref('')
const pageNum = ref(1)
const loading = ref(false)
const dialogVisible = ref(false)

// 统计数据
const statsData = reactive({
  studentCount: 0,
  teacherCount: 0
})

const form = reactive({
  id: null, username: '', name: '', role: '', className: '', password: ''
})

// 生成随机头像背景色
const generateColor = (name) => {
  const colors = [
    'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)',
    'linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%)',
    'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)'
  ]
  if (!name) return colors[4]
  return colors[name.charCodeAt(0) % colors.length]
}

// 切换 Tab
const switchTab = (tab) => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  searchText.value = ''
  pageNum.value = 1
  fetchData()
}

// 获取统计数据
const fetchStats = async () => {
  try {
    const res = await request.get('/stats/counts')
    statsData.studentCount = res.studentCount || 0
    statsData.teacherCount = res.teacherCount || 0
  } catch (e) {
    console.error('获取统计数据失败', e)
  }
}

// 获取用户列表：新增加载状态，优化体验
const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/user/list', {
      params: {
        pageNum: pageNum.value,
        pageSize: 10,
        role: activeTab.value,
        username: searchText.value
      }
    })
    tableData.value = res.records
    total.value = res.total
  } catch (e) {
    ElMessage.error('获取用户列表失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 获取班级列表
const fetchClassList = async () => {
  try {
    const res = await request.get('/class/list')
    classList.value = res
  } catch (e) {
    ElMessage.error('获取班级列表失败')
    console.error(e)
  }
}

// 获取部门列表
const fetchDepartmentList = async () => {
  try {
    const res = await request.get('/department/list')
    departmentList.value = res
  } catch (e) {
    ElMessage.error('获取部门列表失败')
    console.error(e)
  }
}

// 新增用户
const handleAdd = (role) => {
  form.id = null
  form.username = ''
  form.name = ''
  form.role = role
  form.className = ''
  form.password = ''
  dialogVisible.value = true
}

// 编辑用户
const handleEdit = (row) => {
  Object.assign(form, row)
  form.password = null // 编辑时清空密码，不修改原有密码
  dialogVisible.value = true
}

// 删除用户
const handleDelete = async (row) => {
  try {
    await request.delete(`/user/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
    fetchStats()
  } catch (e) {
    ElMessage.error('删除失败')
    console.error(e)
  }
}

// 提交表单（新增/编辑）
const submitForm = async () => {
  try {
    await request.post('/user/save', form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchData()
    fetchStats()
  } catch (error) {
    console.log('提交被拦截:', error)
  }
}

// 新增：处理用户导入上传逻辑
const handleUpload = async (param) => {
  const formData = new FormData()
  formData.append('file', param.file)
  try {
    await request.post('/user/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success('导入成功')
    fetchData() // 导入成功后刷新用户列表
  } catch (e) {
    ElMessage.error('导入失败，请检查文件格式或内容')
    console.error('导入异常：', e)
  }
}

// 初始化加载数据
onMounted(() => {
  fetchData()
  fetchClassList()
  fetchDepartmentList()
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
  cursor: pointer;
  transition: all 0.3s;
}

.ai-stat-box:hover {
  border-color: #CBD5E1;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.ai-stat-box.active {
  border-color: #0694A2;
  background-color: #F0FAFF;
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

.val.success {
  color: #2ED47A;
}

.val.info {
  color: #3498DB;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.tab-buttons { display: flex; gap: 8px; }
.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: none;
  border-radius: 10px;
  background: #f5f7fa;
  color: #606266;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}
.tab-btn:hover {
  background: #e8e8e8;
}
.tab-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}
.search-input {
  width: 260px;
}
.search-input :deep(.el-input__wrapper) {
  border-radius: 10px;
}

/* 用户列表 */
.avatar-wrapper {
  position: relative;
  display: inline-block;
}
.avatar-wrapper .el-avatar {
  font-weight: 600;
}
.online-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 10px;
  height: 10px;
  background: #67c23a;
  border: 2px solid #fff;
  border-radius: 50%;
}
.user-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.user-name {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}
.user-id {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}
.class-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 8px;
}
.no-class {
  color: #c0c4cc;
  font-size: 13px;
}

/* 空状态 */
.empty-state {
  padding: 60px 0;
}

/* 分页 */
.pagination-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0 0;
  border-top: 1px solid #f0f0f0;
  margin-top: 20px;
}
.pagination-info {
  font-size: 13px;
  color: #909399;
}
.total-num {
  color: #667eea;
  font-weight: 600;
}

/* 响应式适配 */
@media (max-width: 1200px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>