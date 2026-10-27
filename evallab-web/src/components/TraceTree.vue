<template>
  <div class="trace-tree">
    <div v-for="node in roots" :key="node.span.span_id" class="node-wrap">
      <TraceNode :node="node" :depth="0" :collapsed="collapsed" @toggle="toggle" />
    </div>
    <div v-if="!roots.length" class="muted">该 Trace 没有 Span</div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import TraceNode from './TraceNode.vue'

const props = defineProps({
  spans: { type: Array, default: () => [] }
})

// 折叠状态按 span_id 记，默认全部展开
const collapsed = ref({})
const toggle = (spanId) => {
  collapsed.value = { ...collapsed.value, [spanId]: !collapsed.value[spanId] }
}

/**
 * 按 parent_span_id 建树。父节点缺失（被裁剪或数据异常）的 span 当作根节点，
 * 否则整棵子树会从界面上消失。
 */
const roots = computed(() => {
  const byId = new Map()
  for (const span of props.spans) {
    byId.set(span.span_id, { span, children: [] })
  }
  const top = []
  for (const node of byId.values()) {
    const parent = node.span.parent_span_id ? byId.get(node.span.parent_span_id) : null
    if (parent) {
      parent.children.push(node)
    } else {
      top.push(node)
    }
  }
  return top
})
</script>

<style scoped>
.trace-tree {
  font-size: 13px;
}
</style>
