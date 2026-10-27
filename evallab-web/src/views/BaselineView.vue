<template>
  <el-card shadow="never" class="block">
    <div class="toolbar">
      <el-input v-model="form.taskId" placeholder="已完成的任务 ID" style="width: 220px" />
      <el-input v-model="form.name" placeholder="基线名称（可留空）" clearable style="width: 220px" />
      <el-button type="primary" :loading="creating" @click="create">创建基线</el-button>
      <span class="muted">从已完成任务快照通过率 / 平均分 / P95</span>
    </div>
  </el-card>

  <el-card v-if="baselines.length" shadow="never" class="block">
    <div class="panel-title">基线列表</div>
    <el-table :data="baselines" stripe>
      <el-table-column prop="name" label="名称" min-width="200" show-overflow-tooltip />
      <el-table-column prop="agent_name" label="Agent" width="180" show-overflow-tooltip />
      <el-table-column label="通过率" width="100">
        <template #default="{ row }">{{ percent(row.pass_rate) }}</template>
      </el-table-column>
      <el-table-column label="平均分" width="100">
        <template #default="{ row }">{{ row.avg_score ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="P95 耗时" width="110">
        <template #default="{ row }">{{ row.p95_duration_ms ? row.p95_duration_ms + ' ms' : '—' }}</template>
      </el-table-column>
      <el-table-column label="用例数" width="90" prop="total_cases" />
      <el-table-column label="来源任务" width="200">
        <template #default="{ row }"><span class="mono muted">{{ row.task_id }}</span></template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-card v-if="baselines.length" shadow="never" class="block">
    <div class="panel-title">回归门禁</div>
    <div class="toolbar">
      <el-input v-model="gate.taskId" placeholder="待判定任务 ID" style="width: 200px" />
      <el-select v-model="gate.baselineId" placeholder="选择基线" style="width: 220px">
        <el-option v-for="b in baselines" :key="b.baseline_id" :label="b.name" :value="b.baseline_id" />
      </el-select>
      <el-input v-model="gate.passRate" placeholder="通过率阈值，如 -0.10" style="width: 180px" />
      <el-input v-model="gate.avgScore" placeholder="平均分阈值，如 -0.15" style="width: 180px" />
      <el-input v-model="gate.p95" placeholder="P95 倍率，如 1.5" style="width: 160px" />
      <el-button type="primary" :loading="gating" @click="runGate">执行门禁</el-button>
    </div>

    <el-alert v-if="gateResult" :type="gateResult.passed ? 'success' : 'error'" :closable="false" show-icon
              :title="gateResult.passed ? '✅ 回归检测通过' : '❌ 回归检测失败'"
              :description="gateResult.reason || ''" style="margin-bottom: 16px" />
    <el-alert v-if="gateResult?.cross_agent" type="warning" :closable="false" show-icon
              title="基线与当前任务来自不同 Agent，结论仅供参考" style="margin-bottom: 16px" />

    <el-table v-if="gateResult" :data="changeRows" stripe>
      <el-table-column prop="label" label="指标" width="120" />
      <el-table-column label="基线" width="140">
        <template #default="{ row }">{{ display(row, row.baseline_value) }}</template>
      </el-table-column>
      <el-table-column label="当前" width="140">
        <template #default="{ row }">{{ display(row, row.current_value) }}</template>
      </el-table-column>
      <el-table-column label="变化" width="140">
        <template #default="{ row }">
          <span :class="{ 'is-regressed': row.regressed }">{{ changeText(row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="阈值" width="130">
        <template #default="{ row }">{{ thresholdText(row) }}</template>
      </el-table-column>
      <el-table-column label="结论" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.regressed" type="danger" size="small">回归</el-tag>
          <el-tag v-else-if="row.baseline_value === null || row.current_value === null"
                  type="info" size="small">跳过</el-tag>
          <el-tag v-else type="success" size="small">正常</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="detail" label="说明" min-width="240" show-overflow-tooltip />
    </el-table>
  </el-card>

  <el-empty v-if="!baselines.length" description="暂无基线：先跑一次评测，再用任务号创建基线" />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { baselineApi } from '../api'

const baselines = ref([])
const form = ref({ taskId: '', name: '' })
const gate = ref({ taskId: '', baselineId: null, passRate: '', avgScore: '', p95: '' })
const gateResult = ref(null)
const creating = ref(false)
const gating = ref(false)

const percent = (v) => (v === null || v === undefined ? '—' : (Number(v) * 100).toFixed(1) + '%')
const formatTime = (v) => (v ? new Date(v).toLocaleString('zh-CN') : '—')

const changeRows = computed(() =>
  Object.entries(gateResult.value?.changes || {}).map(([metric, c]) => ({ metric, ...c }))
)

const isDuration = (row) => row.metric === 'p95_duration_ms'
const display = (row, v) => {
  if (v === null || v === undefined) return '—'
  return isDuration(row) ? Number(v) + ' ms' : Number(v).toFixed(4)
}
const signed = (v, digits) => {
  const n = Number(v)
  return (n >= 0 ? '+' : '') + n.toFixed(digits)
}
const changeText = (row) => {
  if (row.baseline_value === null || row.current_value === null) return '—'
  return isDuration(row)
    ? signed((Number(row.ratio) - 1) * 100, 1) + '%'
    : signed(row.delta, 4)
}
const thresholdText = (row) => {
  if (row.threshold === null || row.threshold === undefined) return '—'
  return isDuration(row)
    ? '+' + (Number(row.threshold) - 1) * 100 + '%'
    : signed(row.threshold, 2)
}

async function load() {
  baselines.value = await baselineApi.list()
}

async function create() {
  if (!form.value.taskId) {
    ElMessage.warning('请填写任务 ID')
    return
  }
  creating.value = true
  try {
    await baselineApi.create({ task_id: form.value.taskId, name: form.value.name || null })
    ElMessage.success('基线已创建')
    form.value = { taskId: '', name: '' }
    await load()
  } finally {
    creating.value = false
  }
}

async function runGate() {
  if (!gate.value.taskId || !gate.value.baselineId) {
    ElMessage.warning('请填写任务 ID 并选择基线')
    return
  }
  // 空字符串不能发给后端：BigDecimal 反序列化会失败，未填的项交给服务端默认值
  const thresholds = {}
  if (gate.value.passRate) thresholds.pass_rate_threshold = gate.value.passRate
  if (gate.value.avgScore) thresholds.avg_score_threshold = gate.value.avgScore
  if (gate.value.p95) thresholds.p95_duration_threshold = gate.value.p95

  gating.value = true
  try {
    gateResult.value = await baselineApi.gate({
      task_id: gate.value.taskId,
      baseline_id: gate.value.baselineId,
      thresholds
    })
  } finally {
    gating.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.panel-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}
.is-regressed {
  color: #f56c6c;
  font-weight: 600;
}
</style>
