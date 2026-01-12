<template>
  <div class="timetable">
    <div class="header">
      <div class="th section-col">节次 / 星期</div>
      <div class="th" v-for="day in weekDays" :key="day">{{ day }}</div>
    </div>
    <div class="body">
      <div class="row" v-for="section in 4" :key="section">
        <div class="td section-col">
          <div class="section-name">第{{ section }}大节</div>
          <div class="section-time">{{ getTimeStr(section) }}</div>
        </div>
        <div class="td" v-for="day in 7" :key="day" @click="handleSlotClick(day, section)">

          <!-- 课程卡片 -->
          <div v-if="getCourse(day, section)" class="course-card" :class="'color-' + getCourse(day, section).id % 5">
            <div class="c-name">{{ getCourse(day, section).courseName }}</div>

            <!-- ★★★ 修复：只有 location 有值才显示这一行 ★★★ -->
            <div class="c-info" v-if="getCourse(day, section).location">
              <el-icon><Location /></el-icon> {{ getCourse(day, section).location }}
            </div>

            <div class="c-info">
              <el-icon><User /></el-icon> {{ getCourse(day, section).className }}
            </div>
          </div>

          <!-- 空白格 -->
          <div v-else class="empty-slot" :class="{ 'hover-effect': canEdit }">
            <el-icon v-if="canEdit" class="add-icon"><Plus /></el-icon>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  data: Array,
  canEdit: Boolean
})

const emit = defineEmits(['click-slot', 'click-course'])

const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const getCourse = (day, section) => {
  return props.data.find(item => item.weekDay === day && item.section === section)
}

const getTimeStr = (section) => {
  const times = ['08:00-09:40', '10:00-11:40', '14:00-15:40', '16:00-17:40']
  return times[section - 1]
}

const handleSlotClick = (day, section) => {
  const course = getCourse(day, section)
  if (course) {
    emit('click-course', course)
  } else if (props.canEdit) {
    emit('click-slot', { weekDay: day, section: section })
  }
}
</script>

<style scoped>
.timetable { border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.02); }
.header { display: grid; grid-template-columns: 80px repeat(7, 1fr); background: #fafafa; border-bottom: 1px solid #e0e0e0; }
.th { padding: 12px; text-align: center; font-weight: bold; color: #606266; border-right: 1px solid #e0e0e0; font-size: 14px; }
.row { display: grid; grid-template-columns: 80px repeat(7, 1fr); border-bottom: 1px solid #e0e0e0; min-height: 110px; }
.row:last-child { border-bottom: none; }
.td { border-right: 1px solid #e0e0e0; position: relative; padding: 4px; }
.td:last-child { border-right: none; }

.section-col { display: flex; flex-direction: column; justify-content: center; align-items: center; background: #fafafa; }
.section-name { font-weight: bold; color: #333; font-size: 14px; }
.section-time { font-size: 12px; color: #909399; margin-top: 4px; }

.course-card {
  height: 100%; width: 100%; border-radius: 6px; cursor: pointer; color: #fff; font-size: 12px;
  display: flex; flex-direction: column; justify-content: center; padding: 0 8px; box-sizing: border-box;
  transition: transform 0.2s, box-shadow 0.2s;
}
.course-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.15); z-index: 2; position: relative; }
.c-name { font-weight: bold; font-size: 13px; margin-bottom: 6px; line-height: 1.3; }
.c-info { display: flex; align-items: center; gap: 4px; opacity: 0.95; margin-bottom: 2px; }

.empty-slot { height: 100%; width: 100%; display: flex; align-items: center; justify-content: center; }
.add-icon { color: #dcdfe6; font-size: 24px; transition: 0.3s; opacity: 0; }
.empty-slot.hover-effect:hover { background-color: #f9faff; border-radius: 6px; }
.empty-slot.hover-effect:hover .add-icon { opacity: 1; color: #409EFF; }

/* 更加柔和的配色 */
.color-0 { background: linear-gradient(135deg, #FF9A9E 0%, #FECFEF 100%); color: #5f2c3e; }
.color-1 { background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%); color: #4a3b52; }
.color-2 { background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%); color: #2c5f4c; }
.color-3 { background: linear-gradient(135deg, #fccb90 0%, #d57eeb 100%); color: #5f3b2c; }
.color-4 { background: linear-gradient(135deg, #e0c3fc 0%, #8ec5fc 100%); color: #3b3b5f; }
</style>