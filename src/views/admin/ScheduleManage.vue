<template>
  <div class="studio-dashboard">

    <!-- 顶部：欢迎栏 (极简风格) -->
    <div class="welcome-header">
      <div class="header-left">
        <h1>排课管理中心</h1>
        <p class="subtitle">点击空白格新增课程，点击课程块编辑或删除</p>
      </div>
      <div class="header-right">
        <el-select
            v-model="filterClass"
            placeholder="请选择班级"
            @change="fetchData"
            v-if="classList.length > 0"
            class="class-select"
        >
          <el-option v-for="c in classList" :key="c.id" :label="c.className" :value="c.className" />
        </el-select>
        <el-select v-else placeholder="加载中..." disabled class="class-select">
          <el-option value="" label="班级加载中..." />
        </el-select>
      </div>
    </div>

    <!-- 核心布局：Grid System -->
    <div class="dashboard-grid">

      <!-- 左侧：主要内容区 (占 2/3) -->
      <div class="main-column">

        <!-- 课表卡片 -->
        <div class="studio-card">
          <div class="card-header-row">
            <div class="section-title">课程安排表</div>
          </div>
          <TimeTable
              :data="scheduleData"
              :can-edit="true"
              @click-slot="handleAdd"
              @click-course="handleEdit"
          />
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
            <div class="tool-item" @click="handleAdd({weekDay: 1, section: 1})">
              <div class="tool-icon" style="background: #E6F4FF; color: #0694A2">
                <el-icon><Plus /></el-icon>
              </div>
              <span class="tool-name">新增课程</span>
            </div>
            <div class="tool-item" @click="fetchData">
              <div class="tool-icon" style="background: #F3F4F6; color: #64748B">
                <el-icon><Refresh /></el-icon>
              </div>
              <span class="tool-name">刷新课表</span>
            </div>
          </div>
        </div>

        <!-- 课表统计信息 -->
        <div class="studio-card side-widget">
          <div class="widget-header">
            <span>课表统计</span>
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="ai-grid">
            <div class="ai-stat-box">
              <span class="label">总课程数</span>
              <span class="val primary">{{ scheduleData.length }}</span>
            </div>
            <div class="ai-stat-box">
              <span class="label">当前班级</span>
              <span class="val warning">{{ filterClass || '未选择' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑/新增弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑课程' : '新增排课'" width="520px" align-center class="schedule-dialog">
      <el-form :model="form" label-width="100px" class="schedule-form">
        <el-form-item label="时间坐标">
          <div class="time-coord">
            <el-tag effect="dark" type="primary" round>周{{ weekMap[form.weekDay] }}</el-tag>
            <el-tag effect="dark" type="warning" round>第 {{ form.section }} 大节</el-tag>
          </div>
        </el-form-item>

        <el-form-item label="课程名称">
          <el-select
              v-model="form.courseName"
              allow-create
              filterable
              default-first-option
              placeholder="选择或直接输入新课程"
              style="width: 100%"
          >
            <el-option 
              v-for="course in courseList" 
              :key="course" 
              :label="course" 
              :value="course" 
            />
          </el-select>
        </el-form-item>

        <el-form-item label="上课班级">
          <el-select v-model="form.className" style="width: 100%" v-if="classList.length > 0">
            <el-option v-for="c in classList" :key="c.id" :label="c.className" :value="c.className" />
          </el-select>
          <el-select v-else placeholder="班级加载中..." style="width: 100%" disabled />
        </el-form-item>

        <el-form-item label="任课老师">
          <el-select v-model="form.teacherId" placeholder="请选择老师" filterable style="width: 100%" v-if="teacherList.length > 0">
            <el-option
                v-for="t in teacherList"
                :key="t.id"
                :label="t.name + ' (' + t.username + ')'"
                :value="t.id"
            />
          </el-select>
          <el-select v-else placeholder="老师加载中..." style="width: 100%" disabled />
        </el-form-item>

        <el-form-item label="上课地点">
          <el-input v-model="form.location" placeholder="例如: 3号楼201" prefix-icon="Location" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button v-if="form.id" type="danger" plain @click="handleDelete">
            <el-icon><Delete /></el-icon> 删除课程
          </el-button>
          <div class="footer-right">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitForm">
              <el-icon><Check /></el-icon> 保存排课
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import TimeTable from '../../components/TimeTable.vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Calendar,
  Delete,
  Check,
  Plus,
  Refresh,
  Grid,
  DataAnalysis
} from '@element-plus/icons-vue'

const scheduleData = ref([])
const classList = ref([])
const teacherList = ref([])
const courseList = ref([])  // 添加课程列表
const filterClass = ref('')
const dialogVisible = ref(false)

const weekMap = {1:'一', 2:'二', 3:'三', 4:'四', 5:'五', 6:'六', 7:'日'}

const form = reactive({
  id: null, courseName: '', teacherId: '', className: '', location: '', weekDay: 1, section: 1
})

// 加载课表数据
const fetchData = async () => {
  if (!filterClass.value && classList.value.length > 0) {
    filterClass.value = classList.value[0].className // 默认选中第一个班级
  }
  const res = await request.get('/schedule/list', { params: { className: filterClass.value } })
  scheduleData.value = res || []
}

// 加载基础数据 (班级 + 老师 + 课程)
const initBaseData = async () => {
  // 1. 查班级
  const classes = await request.get('/class/list')
  classList.value = classes || []

  // 2. 查老师 (复用用户列表接口，传 role=TEACHER, pageSize大一点查全部)
  const teachers = await request.get('/user/list', {params: {role: 'TEACHER', pageSize: 100}})
  teacherList.value = teachers.records || []

  // 3. 查课程 (获取所有已存在的课程名称)
  const courses = await request.get('/schedule/courses')
  courseList.value = courses || []

  // 4. 加载默认班级的课表
  fetchData()
}

// 点击空白处：新增
const handleAdd = (slot) => {
  form.id = null
  form.weekDay = slot.weekDay
  form.section = slot.section
  // 自动填入当前筛选的班级
  form.className = filterClass.value || ''
  form.courseName = ''
  form.teacherId = ''
  form.location = ''

  // eslint-disable-next-line
  dialogVisible.value = true
}

// 点击课程：编辑
const handleEdit = (course) => {
  Object.assign(form, course)
  // eslint-disable-next-line
  dialogVisible.value = true
}

// 提交保存
const submitForm = async () => {
  if (!form.courseName || !form.teacherId || !form.className) {
    return ElMessage.warning('请填写完整信息')
  }
  try {
    await request.post('/schedule/save', form)
    ElMessage.success('排课成功')
    // eslint-disable-next-line
    dialogVisible.value = false
    fetchData() // 刷新课表
    
    // 刷新课程列表，确保新课程出现在下拉框中
    const courses = await request.get('/schedule/courses')
    courseList.value = courses || []
  } catch (e) {
    // 错误在 request.js 已处理 (比如冲突检测)
  }
}

// 删除课程
const handleDelete = async () => {
  ElMessageBox.confirm('确定要删除这节课吗？', '警告', {type: 'warning'}).then(async () => {
    await request.delete(`/schedule/${form.id}`)
    ElMessage.success('删除成功')
    // eslint-disable-next-line
    dialogVisible.value = false
    fetchData()
    
    // 刷新课程列表
    const courses = await request.get('/schedule/courses')
    courseList.value = courses || []
  })
}

onMounted(() => {
  initBaseData()
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

/* 课表卡片 - 统一仪表盘卡片风格 */
.timetable-card {
  border-radius: 24px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  padding: 20px;
  background: #fff;
  transition: all 0.3s;
}

.timetable-card:hover {
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.05);
}

/* 弹窗样式重构 */
.schedule-dialog :deep(.el-dialog) {
  border-radius: 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  border: none;
  padding: 0;
}

.schedule-dialog :deep(.el-dialog__header) {
  padding: 24px 24px 0;
  border-bottom: 1px solid #F1F5F9;
}

.schedule-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 700;
  color: #1E293B;
}

.schedule-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.schedule-dialog :deep(.el-dialog__footer) {
  padding: 0 24px 24px;
  border-top: 1px solid #F1F5F9;
}

/* 表单样式 */
.schedule-form {
  padding: 0;
}

.schedule-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.schedule-form :deep(.el-form-item__label) {
  font-size: 14px;
  font-weight: 600;
  color: #64748B;
}

/* 时间坐标标签 */
.time-coord {
  display: flex;
  gap: 12px;
  align-items: center;
}

.time-coord :deep(.el-tag) {
  border-radius: 30px;
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 600;
}

.time-coord :deep(.el-tag--primary) {
  background: #E6F4FF;
  color: #0694A2;
  border: none;
}

.time-coord :deep(.el-tag--warning) {
  background: #FFF4E6;
  color: #FF9F43;
  border: none;
}

/* 表单控件样式统一 */
.schedule-form :deep(.el-select .el-input__wrapper),
.schedule-form :deep(.el-input .el-input__wrapper) {
  border-radius: 16px;
  border: 1px solid #F1F5F9;
  padding: 4px 12px;
  box-shadow: none;
  transition: all 0.2s;
}

.schedule-form :deep(.el-select .el-input__wrapper:hover),
.schedule-form :deep(.el-input .el-input__wrapper:hover) {
  border-color: #0694A2;
  box-shadow: 0 0 0 2px rgba(6, 148, 162, 0.1);
}

.schedule-form :deep(.el-select .el-input__wrapper.is-focus),
.schedule-form :deep(.el-input .el-input__wrapper.is-focus) {
  border-color: #0694A2;
  box-shadow: 0 0 0 2px rgba(6, 148, 162, 0.2);
}

/* 弹窗按钮区域 */
.dialog-footer {
  display: flex;
  justify-content: space-between;
  width: 100%;
  align-items: center;
  padding-top: 16px;
}

.footer-right {
  display: flex;
  gap: 12px;
}

/* 按钮样式统一 */
.dialog-footer :deep(.el-button) {
  border-radius: 12px;
  padding: 8px 20px;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.2s;
}

.dialog-footer :deep(.el-button--primary) {
  background: #0694A2;
  border-color: #0694A2;
}

.dialog-footer :deep(.el-button--primary:hover) {
  background: #05828f;
  border-color: #05828f;
  box-shadow: 0 4px 12px rgba(6, 148, 162, 0.3);
}

.dialog-footer :deep(.el-button--danger) {
  color: #FF6B6B;
  border-color: #FFE4E6;
  background: #FFF0F1;
}

.dialog-footer :deep(.el-button--danger:hover) {
  background: #FFE4E6;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
}

.dialog-footer :deep(.el-button--default) {
  color: #64748B;
  border-color: #E2E8F0;
  background: #F8FAFC;
}

.dialog-footer :deep(.el-button--default:hover) {
  background: #F1F5F9;
  border-color: #CBD5E1;
}

/* 响应式适配 */
@media (max-width: 1200px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>