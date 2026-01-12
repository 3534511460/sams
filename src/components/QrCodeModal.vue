<!-- src/components/QrCodeModal.vue -->
<template>
  <el-dialog
      v-model="visible"
      title="📱 扫码签到"
      width="400px"
      align-center
      destroy-on-close
      @close="handleClose"
  >
    <div class="qr-container" v-loading="loading">
      <div v-if="text" class="qr-wrapper">
        <!-- canvas 用于绘制二维码 -->
        <canvas id="qr-canvas"></canvas>
        <p class="code-text">{{ text }}</p>
        <p class="desc">请让学生扫描上方二维码或输入口令</p>
      </div>
      <div v-else class="empty">
        暂无数据
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch, nextTick, computed } from 'vue'
import QRCode from 'qrcode'

// 接收父组件参数
const props = defineProps({
  modelValue: Boolean, // 控制显示隐藏 (v-model)
  text: String         // 二维码内容
})

const emit = defineEmits(['update:modelValue'])

// 计算属性做双向绑定代理
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)

// 监听显示状态，一旦显示，就去画图
watch(() => props.modelValue, async (val) => {
  if (val && props.text) {
    await generateQR()
  }
})

const generateQR = async () => {
  loading.value = true
  await nextTick() // 等待 DOM 渲染出 canvas 标签

  const canvas = document.getElementById('qr-canvas')
  if (canvas) {
    try {
      await QRCode.toCanvas(canvas, props.text, {
        width: 250,
        margin: 1,
        color: { dark: '#000000', light: '#ffffff' }
      })
    } catch (err) {
      console.error(err)
    }
  }
  loading.value = false
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.qr-container { display: flex; justify-content: center; align-items: center; padding: 10px; }
.qr-wrapper { text-align: center; }
.code-text { font-size: 32px; font-weight: bold; color: #409EFF; margin: 10px 0; letter-spacing: 5px; }
.desc { color: #909399; font-size: 14px; }
</style>