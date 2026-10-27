<template>
  <el-card v-loading="loading" shadow="never">
    <div class="toolbar">
      <el-button size="small" @click="$router.back()">← 返回</el-button>
      <span class="mono muted">{{ traceId }}</span>
      <el-tag v-if="trace" :type="statusType(trace.status)" size="small">{{ trace.status }}</el-tag>
      <el-tag v-if="trace" size="small" type="info">{{ trace.source }}</el-tag>
    </div>

    <template v-if="trace">
      <div class="stats">
        <div class="stat">
          <div class="stat-label">Agent</div>
          <div class="stat-value">{{ trace.agent_name }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">耗时</div>
          <div class="stat-value">{{ trace.duration_ms }} ms</div>
        </div>
        <div class="stat">
          <div class="stat-label">Token</div>
          <div class="stat-value">{{ trace.total_tokens }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">Span 数</div>
          <div class="stat-value">{{ trace.spans.length }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">错误 Span</div>
          <div class="stat-value" :class="{ danger: errorCount > 0 }">{{ errorCount }}</div>
        </div>
      </div>

      <div class="panel-title">输出</div>
      <pre class="output">{{ trace.output || '（空）' }}</pre>

      <div class="panel-title">Span 树</div>
      <TraceTree :spans="trace.spans" />
    </template>

    <el-empty v-else-if="!loading" description="Trace 不存在" />
  </el-card>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { traceApi } from '../api'
import TraceTree from '../components/TraceTree.vue'

const route = useRoute()
const traceId = computed(() => route.params.traceId)
const trace = ref(null)
const loading = ref(false)

const statusType = (s) => ({ SUCCESS: 'success', FAILED: 'danger', TIMEOUT: 'warning' })[s] || 'info'
const errorCount = computed(() => (trace.value?.spans || []).filter((s) => !!s.error).length)

async function load() {
  loading.value = true
  try {
    trace.value = await traceApi.get(traceId.value)
  } catch (e) {
    trace.value = null
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(traceId, load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}
.stats {
  display: flex;
  gap: 40px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.stat-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 6px;
}
.stat-value {
  font-size: 18px;
  font-weight: 600;
}
.stat-value.danger {
  color: #f56c6c;
}
.panel-title {
  font-size: 14px;
  font-weight: 600;
  margin: 16px 0 8px;
}
.output {
  margin: 0;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}
</style>
