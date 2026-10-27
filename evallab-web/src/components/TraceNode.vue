<template>
  <div class="node">
    <div class="row" :class="{ error: !!node.span.error }" :style="{ paddingLeft: depth * 20 + 'px' }">
      <button v-if="node.children.length" class="toggle" :title="isCollapsed ? '展开' : '折叠'"
              @click="$emit('toggle', node.span.span_id)">
        {{ isCollapsed ? '▸' : '▾' }}
      </button>
      <span v-else class="toggle placeholder"></span>

      <el-tag size="small" :type="typeColor(node.span.span_type)" class="type">
        {{ node.span.span_type }}
      </el-tag>
      <span class="name">{{ node.span.tool_name || label(node.span.span_type) }}</span>
      <span class="muted mono">{{ node.span.duration_ms }} ms</span>
      <span v-if="node.span.tokens" class="muted mono">{{ node.span.tokens }} tok</span>
      <el-tag v-if="node.span.error" size="small" type="danger">error</el-tag>
      <span v-if="node.children.length" class="muted">{{ node.children.length }} 子节点</span>
    </div>

    <div v-if="!isCollapsed" class="body" :style="{ paddingLeft: depth * 20 + 32 + 'px' }">
      <div v-if="node.span.error" class="error-text">{{ node.span.error }}</div>
      <details v-if="node.span.input" class="io">
        <summary>输入</summary>
        <pre>{{ node.span.input }}</pre>
      </details>
      <details v-if="node.span.output" class="io">
        <summary>输出</summary>
        <pre>{{ node.span.output }}</pre>
      </details>
      <div v-if="node.span.events?.length" class="events">
        <div v-for="(e, i) in node.span.events" :key="i" class="event">
          <el-tag size="small" :type="e.event_type === 'ERROR' ? 'danger' : e.event_type === 'WARNING' ? 'warning' : 'info'">
            {{ e.event_type }}
          </el-tag>
          <span>{{ e.message }}</span>
        </div>
      </div>
    </div>

    <TraceNode v-for="child in node.children" :key="child.span.span_id" :node="child" :depth="depth + 1"
               :collapsed="collapsed" @toggle="$emit('toggle', $event)" />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  collapsed: { type: Object, default: () => ({}) }
})
defineEmits(['toggle'])

const isCollapsed = computed(() => !!props.collapsed[props.node.span.span_id])

const typeColor = (type) => ({ LLM_CALL: 'warning', TOOL_CALL: 'success', RETRIEVAL: 'info' })[type] || 'info'
const label = (type) => ({ LLM_CALL: 'LLM 调用', TOOL_CALL: '工具调用', RETRIEVAL: '检索' })[type] || type
</script>

<style scoped>
.row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 4px;
}
.row:hover {
  background: #f5f7fa;
}
.row.error {
  background: #fef0f0;
}
.toggle {
  width: 16px;
  border: none;
  background: none;
  cursor: pointer;
  color: #909399;
  padding: 0;
}
.toggle.placeholder {
  cursor: default;
}
.type {
  width: 78px;
  text-align: center;
}
.name {
  font-weight: 500;
  min-width: 120px;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}
.body {
  padding-bottom: 4px;
}
.error-text {
  color: #f56c6c;
  margin-bottom: 4px;
}
.io {
  margin: 2px 0;
}
.io summary {
  cursor: pointer;
  color: #909399;
  font-size: 12px;
}
.io pre {
  margin: 4px 0;
  padding: 6px 8px;
  background: #f5f7fa;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
}
.events {
  margin-top: 4px;
}
.event {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 2px 0;
  font-size: 12px;
}
</style>
