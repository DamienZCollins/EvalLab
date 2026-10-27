<template>
  <el-card shadow="never" class="block">
    <div class="toolbar">
      <el-input v-model="form.agentName" placeholder="被测 Agent 名称" style="width: 220px" />
      <el-select v-model="form.groupId" placeholder="全部分组" clearable style="width: 160px">
        <el-option v-for="g in groups" :key="g.group_id" :label="g.group_name" :value="g.group_id" />
      </el-select>
      <el-input v-model="form.tag" placeholder="按标签筛选" clearable style="width: 160px" />
      <el-button type="primary" :loading="starting" @click="start">开始评测</el-button>
      <span class="muted">按筛选条件取启用用例，逐例拉取 Agent 评分</span>
    </div>
  </el-card>

  <el-card v-if="task" shadow="never" class="block">
    <div class="stats">
      <div class="stat">
        <div class="stat-label">状态</div>
        <el-tag :type="statusType(task.status)">{{ task.status }}</el-tag>
      </div>
      <div class="stat">
        <div class="stat-label">通过 / 总数</div>
        <div class="stat-value">{{ task.passed_cases }} / {{ task.total_cases }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">通过率</div>
        <div class="stat-value">{{ percent(task.pass_rate) }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">平均分</div>
        <div class="stat-value">{{ task.avg_score ?? '—' }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">P95 耗时</div>
        <div class="stat-value">{{ task.p95_duration_ms ? task.p95_duration_ms + ' ms' : '—' }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">任务号</div>
        <div class="stat-value mono">{{ task.task_id }}</div>
      </div>
    </div>
    <el-alert v-if="task.error" :title="task.error" type="error" :closable="false" show-icon />

    <el-table :data="results" v-loading="loadingResults" stripe style="margin-top: 16px">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="scores">
            <div v-for="s in row.scores" :key="s.score_name" class="score-row">
              <el-tag size="small" :type="s.passed === false ? 'danger' : s.passed ? 'success' : 'info'">
                {{ s.scorer_type }}
              </el-tag>
              <span class="score-name">{{ s.score_name }}</span>
              <span class="mono">{{ s.score }}</span>
              <span v-if="s.threshold !== null" class="muted">阈值 {{ s.threshold }}</span>
              <span class="muted score-reason">{{ s.reason }}</span>
            </div>
            <div v-if="!row.scores.length" class="muted">无评分记录</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="用例" min-width="200">
        <template #default="{ row }">
          {{ caseName(row.testcase_id) }}
          <span class="muted">v{{ row.testcase_version }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结果" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PASS' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="失败原因" min-width="240" show-overflow-tooltip>
        <template #default="{ row }">
          <span :class="{ muted: !row.failure_reason }">{{ row.failure_reason || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="归因" width="120">
        <template #default="{ row }">
          <span class="muted">{{ row.failure_category || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="耗时" width="110">
        <template #default="{ row }">{{ row.duration_ms }} ms</template>
      </el-table-column>
      <el-table-column label="平均分" width="100">
        <template #default="{ row }">{{ row.avg_score ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="Trace" width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="mono muted">{{ row.trace_id || '—' }}</span>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-empty v-else description="创建任务后在此查看结果" />
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { evalApi, testcaseApi } from '../api'

const form = ref({ agentName: 'fault-injection-agent', groupId: null, tag: '' })
const groups = ref([])
const caseNames = ref({})
const task = ref(null)
const results = ref([])
const starting = ref(false)
const loadingResults = ref(false)
let timer = null

const statusType = (s) => ({ COMPLETED: 'success', FAILED: 'danger', RUNNING: 'warning' })[s] || 'info'
const percent = (v) => (v === null || v === undefined ? '—' : (Number(v) * 100).toFixed(1) + '%')
const caseName = (id) => caseNames.value[id] || `用例 #${id}`

async function start() {
  if (!form.value.agentName) {
    ElMessage.warning('请填写被测 Agent 名称')
    return
  }
  starting.value = true
  try {
    task.value = await evalApi.createTask({
      agent_name: form.value.agentName,
      group_id: form.value.groupId || null,
      tag: form.value.tag || null
    })
    results.value = []
    schedulePoll()
  } finally {
    starting.value = false
  }
}

async function refresh() {
  if (!task.value) return
  task.value = await evalApi.getTask(task.value.task_id)
  loadingResults.value = true
  try {
    results.value = await evalApi.results(task.value.task_id)
  } finally {
    loadingResults.value = false
  }
  if (task.value.status === 'PENDING' || task.value.status === 'RUNNING') {
    schedulePoll()
  } else {
    clearTimer()
  }
}

function schedulePoll() {
  clearTimer()
  timer = setTimeout(refresh, 1500)
}

function clearTimer() {
  if (timer) {
    clearTimeout(timer)
    timer = null
  }
}

onMounted(async () => {
  groups.value = await testcaseApi.groups()
  const list = await testcaseApi.list({ enabled_only: true })
  caseNames.value = Object.fromEntries(list.map((c) => [c.testcase_id, c.name]))
})

onBeforeUnmount(clearTimer)
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.stats {
  display: flex;
  gap: 40px;
  flex-wrap: wrap;
}
.stat-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 6px;
}
.stat-value {
  font-size: 20px;
  font-weight: 600;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}
.scores {
  padding: 8px 24px;
}
.score-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
}
.score-name {
  font-weight: 500;
  min-width: 140px;
}
.score-reason {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
