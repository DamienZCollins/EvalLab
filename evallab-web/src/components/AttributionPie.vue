<template>
  <div ref="chartEl" class="chart"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  // [{ name, value }]，已按分层聚合好的失败数
  data: { type: Array, required: true }
})

const chartEl = ref(null)
let chart = null

const LABELS = {
  ENVIRONMENT: '数据/环境问题',
  TOOL_CALL: '工具调用错误',
  INPUT_MISUNDERSTAND: '输入理解错误',
  PLANNING: '规划错误',
  OUTPUT_GENERATION: '输出生成错误',
  UNKNOWN: '未分类'
}

function render() {
  if (!chart) return
  const data = props.data.map((d) => ({ name: LABELS[d.name] || d.name, value: d.value }))
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 条 ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}\n{c}' },
      data: data.length ? data : [{ name: '无失败用例', value: 1 }]
    }]
  }, true)
}

onMounted(() => {
  chart = echarts.init(chartEl.value)
  render()
  window.addEventListener('resize', handleResize)
})

function handleResize() {
  chart?.resize()
}

watch(() => props.data, render, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.chart {
  width: 100%;
  height: 280px;
}
</style>
