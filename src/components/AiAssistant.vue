<template>
  <div class="ai-assistant">
    <!-- 悬浮按钮 -->
    <div class="ai-fab" @click="toggleChat" :class="{ active: isOpen }">
      <div class="fab-pulse"></div>
      <el-icon v-if="!isOpen" class="fab-icon"><ChatDotRound /></el-icon>
      <el-icon v-else class="fab-icon"><Close /></el-icon>
    </div>

    <!-- 聊天窗口 -->
    <transition name="chat-slide">
      <div v-if="isOpen" class="chat-window">
        <div class="chat-header">
          <div class="header-info">
            <div class="ai-avatar">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="ai-info">
              <span class="ai-name">SAMS 智能助手</span>
              <span class="ai-status"><span class="status-dot"></span> 在线</span>
            </div>
          </div>
          <el-button text circle @click="clearMessages">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>

        <div class="chat-body" ref="chatBodyRef">
          <!-- 欢迎消息 -->
          <div class="message ai-message" v-if="messages.length === 0">
            <div class="message-avatar">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="message-content">
              <p>你好！我是 SAMS 智能助手 🤖</p>
              <p>我可以帮你：</p>
              <ul>
                <li>查询考勤相关问题</li>
                <li>了解请假流程</li>
                <li>分析出勤数据</li>
                <li>解答系统使用问题</li>
              </ul>
              <p>请问有什么可以帮助你的？</p>
            </div>
          </div>

          <!-- 消息列表 -->
          <div v-for="(msg, index) in messages" :key="index"
               class="message" :class="msg.role === 'user' ? 'user-message' : 'ai-message'">
            <div class="message-avatar">
              <el-icon v-if="msg.role === 'ai'"><Monitor /></el-icon>
              <span v-else>{{ userStore.user.name?.charAt(0) || 'U' }}</span>
            </div>
            <div class="message-content">
              <div v-html="formatMessage(msg.content)"></div>
              <span class="message-time">{{ msg.time }}</span>
            </div>
          </div>

          <!-- AI正在输入 -->
          <div v-if="isTyping" class="message ai-message">
            <div class="message-avatar">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="message-content typing">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
            </div>
          </div>
        </div>

        <!-- 快捷问题 -->
        <div class="quick-questions" v-if="messages.length === 0">
          <span class="quick-title">快捷提问：</span>
          <div class="quick-tags">
            <el-tag v-for="q in quickQuestions" :key="q" @click="sendQuickQuestion(q)"
                    effect="plain" round class="quick-tag">{{ q }}</el-tag>
          </div>
        </div>

        <div class="chat-footer">
          <el-input
              v-model="inputText"
              placeholder="输入问题..."
              @keyup.enter="sendMessage"
              :disabled="isTyping"
          >
            <template #append>
              <el-button type="primary" @click="sendMessage" :loading="isTyping">
                <el-icon><Promotion /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick, computed } from 'vue'
import { useUserStore } from '../store/user'
import request from '../utils/request'

const userStore = useUserStore()
const isOpen = ref(false)
const inputText = ref('')
const messages = ref([])
const isTyping = ref(false)
const chatBodyRef = ref(null)

const quickQuestions = [
  '怎么签到？',
  '如何请假？',
  '查看我的出勤率',
  '今天有什么课？'
]

const toggleChat = () => {
  isOpen.value = !isOpen.value
}

const clearMessages = () => {
  messages.value = []
}

const formatMessage = (content) => {
  // 简单的Markdown转换
  return content
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\n/g, '<br>')
}

const getCurrentTime = () => {
  const now = new Date()
  return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  })
}

const sendQuickQuestion = (question) => {
  inputText.value = question
  sendMessage()
}

const sendMessage = async () => {
  if (!inputText.value.trim() || isTyping.value) return

  const userMessage = inputText.value.trim()
  messages.value.push({
    role: 'user',
    content: userMessage,
    time: getCurrentTime()
  })
  inputText.value = ''
  scrollToBottom()

  isTyping.value = true

  try {
    const res = await request.post('/ai/chat', {
      message: userMessage,
      userId: userStore.user.id,
      role: userStore.user.role,
      className: userStore.user.className
    })

    messages.value.push({
      role: 'ai',
      content: res.reply || '抱歉，我暂时无法回答这个问题。',
      time: getCurrentTime()
    })
  } catch (e) {
    // 如果后端接口未实现，使用本地智能回复
    const reply = getLocalReply(userMessage)
    messages.value.push({
      role: 'ai',
      content: reply,
      time: getCurrentTime()
    })
  } finally {
    isTyping.value = false
    scrollToBottom()
  }
}

// 本地智能回复（备用）
const getLocalReply = (question) => {
  const q = question.toLowerCase()

  if (q.includes('签到') || q.includes('打卡')) {
    if (userStore.user.role === 'STUDENT') {
      return '**签到方法：**\n1. 点击左侧菜单"我要签到"\n2. 输入老师提供的签到口令\n3. 点击"确认签到"按钮\n\n⚠️ 注意：迟到超过15分钟将无法签到哦！'
    } else {
      return '**发起签到：**\n1. 点击左侧菜单"发起签到"\n2. 选择课程和目标班级\n3. 设置签到时长\n4. 点击"发布签到"\n\n系统会自动生成签到口令，学生凭口令进行签到。'
    }
  }

  if (q.includes('请假') || q.includes('假')) {
    if (userStore.user.role === 'STUDENT') {
      return '**请假流程：**\n1. 点击左侧菜单"我的请假"\n2. 点击"新增请假"按钮\n3. 填写请假原因和时间\n4. 提交等待审批\n\n审批结果会在页面显示，请关注状态变化。'
    } else {
      return '**审批请假：**\n1. 点击左侧菜单"请假审批"\n2. 查看待审批的请假申请\n3. 点击"通过"或"驳回"进行处理'
    }
  }

  if (q.includes('出勤率') || q.includes('考勤')) {
    return `**你的考勤概况：**
根据系统记录，你可以在"考勤记录"页面查看详细的出勤情况，包括：
- 正常签到次数
- 迟到次数
- 缺勤次数
- 请假次数

点击左侧"考勤记录"即可查看完整数据。`
  }

  if (q.includes('课') || q.includes('课表')) {
    return '**查看课表：**\n点击左侧菜单"我的课表"，可以看到本周所有课程安排，包括：\n- 课程名称\n- 上课时间\n- 上课地点\n- 任课老师'
  }

  if (q.includes('密码') || q.includes('修改')) {
    return '**修改密码：**\n1. 点击右上角头像\n2. 选择"个人中心"\n3. 切换到"安全中心"标签\n4. 输入旧密码和新密码\n5. 点击"确认修改"'
  }

  return `你好！我是SAMS智能助手。

关于"${question}"，建议你：
1. 查看左侧菜单的相关功能
2. 或者换个方式描述你的问题

我可以帮助你了解签到、请假、课表、考勤等相关操作。`
}
</script>

<style scoped>
.ai-assistant {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
}

/* 悬浮按钮 */
.ai-fab {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
  position: relative;
}
.ai-fab:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 30px rgba(102, 126, 234, 0.5);
}
.ai-fab.active {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}
.fab-icon {
  font-size: 26px;
  color: #fff;
}
.fab-pulse {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0% { transform: scale(1); opacity: 0.8; }
  50% { transform: scale(1.3); opacity: 0; }
  100% { transform: scale(1); opacity: 0; }
}

/* 聊天窗口 */
.chat-window {
  position: absolute;
  bottom: 70px;
  right: 0;
  width: 380px;
  height: 520px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 10px 50px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-slide-enter-active, .chat-slide-leave-active {
  transition: all 0.3s ease;
}
.chat-slide-enter-from, .chat-slide-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

/* 头部 */
.chat-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
}
.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ai-avatar {
  width: 40px;
  height: 40px;
  background: rgba(255,255,255,0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}
.ai-name {
  font-weight: 600;
  font-size: 15px;
  display: block;
}
.ai-status {
  font-size: 12px;
  opacity: 0.9;
  display: flex;
  align-items: center;
  gap: 4px;
}
.status-dot {
  width: 8px;
  height: 8px;
  background: #67c23a;
  border-radius: 50%;
  animation: blink 1.5s infinite;
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 消息体 */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f9fafb;
}
.chat-body::-webkit-scrollbar {
  width: 4px;
}
.chat-body::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 4px;
}

/* 消息样式 */
.message {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.user-message {
  flex-direction: row-reverse;
}
.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 14px;
  font-weight: 600;
}
.ai-message .message-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}
.user-message .message-avatar {
  background: #409eff;
  color: #fff;
}
.message-content {
  max-width: 260px;
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.6;
  position: relative;
}
.ai-message .message-content {
  background: #fff;
  border-radius: 16px 16px 16px 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.user-message .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 16px 16px 4px 16px;
}
.message-content p {
  margin: 0 0 8px 0;
}
.message-content p:last-child {
  margin-bottom: 0;
}
.message-content ul {
  margin: 8px 0;
  padding-left: 20px;
}
.message-content li {
  margin-bottom: 4px;
}
.message-time {
  font-size: 11px;
  opacity: 0.6;
  display: block;
  margin-top: 6px;
}

/* 输入中动画 */
.typing {
  display: flex;
  gap: 4px;
  padding: 16px !important;
}
.dot {
  width: 8px;
  height: 8px;
  background: #667eea;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

/* 快捷问题 */
.quick-questions {
  padding: 8px 16px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}
.quick-title {
  font-size: 12px;
  color: #909399;
  display: block;
  margin-bottom: 8px;
}
.quick-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.quick-tag {
  cursor: pointer;
  transition: all 0.2s;
}
.quick-tag:hover {
  background: #667eea;
  color: #fff;
  border-color: #667eea;
}

/* 底部输入 */
.chat-footer {
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}
.chat-footer :deep(.el-input__wrapper) {
  border-radius: 20px;
}
.chat-footer :deep(.el-input-group__append) {
  border-radius: 0 20px 20px 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  padding: 0 16px;
}
.chat-footer :deep(.el-input-group__append .el-button) {
  color: #fff;
}
</style>
