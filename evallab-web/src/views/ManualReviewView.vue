<template>
  <el-card shadow="never" class="block">
    <div class="toolbar">
      <el-input v-model="taskId" placeholder="评测任务号" style="width: 240px" />
      <el-button type="primary" :loading="loading" @click="loadTask">载入结果</el-button>
      <el-button :loading="loading" @click="loadReport">刷新准确率</el-button>
      <span class="muted">对每条结果给出人工判断，用于计算 Judge 与归因的准确率</span>
    </div>

    <el-row :gutter="16" v-if="metrics">
      <el-col :span="6">
        <div class="stat">
          <div class="stat-label">抽检条数</div>
          <div class="stat-value">{{ metrics.total_reviews }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat">
          <div class="stat-label">Judge 准确率</div>
          <div class="stat-value">{{ pct(metrics.judge_accuracy) }}</div>
          <div class="muted">{{ metrics.judge_consistent }} / {{ metrics.judge_samples }} 一致（分差 &lt; 0.1）</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat">
          <div class="stat-label">归因准确率</div>
          <div class="stat-value">{{ pct(metrics.attribution_accuracy) }}</div>
          <div class="muted">{{ metrics.attribution_consistent }} / {{ metrics.attribution_samples }} 一致</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat">
          <div class="stat-label">目标</div>
          <div class="stat-value">≥ 75%</div>
          <div class="muted">低于目标时先查归因规则与 Prompt</div>
        </div>
      </el-col>
    </el-row>
  </el-card>

  <el-card v-if="results.length" shadow="never">
    <el-table :data="results" v-loading="loading" stripe>
      <el-table-column label="用例" min-width="200">
        <template #default="{ row }">
          {{ caseName(row.testcase_id) }}
          <span class="muted">v{{ row.testcase_version }}</span>
        </template>
      </el-table-column>
      <el-table-column label="系统判定" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PASS' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="系统归因" width="130">
        <template #default="{ row }">
          <el-tag v-if="row.failure_category" size="small" type="warning">
            {{ labels[row.failure_category] }}
          </el-tag>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="Judge 分" width="90">
        <template #default="{ row }">{{ judgeOf(row) ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="人工评分" width="130">
        <template #default="{ row }">
          <el-input-number v-model="draftOf(row).human_score" :min="0" :max="1" :step="0.05"
                           :precision="2" size="small" controls-position="right" />
        </template>
      </el-table-column>
      <el-table-column label="人工归因" width="170">
        <template #default="{ row }">
          <el-select v-model="draftOf(row).human_failure_category" placeholder="判定通过" clearable size="small">
            <el-option v-for="(label, value) in labels" :key="value" :label="label" :value="value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="160">
        <template #default="{ row }">
          <el-input v-model="draftOf(row).remark" size="small" placeholder="可选" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :loading="savingId === row.result_id"
                     @click="submit(row)">提交</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-empty v-else-if="!metrics" description="输入任务号载入结果开始抽检" />
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { evalApi, reviewApi, testcaseApi } from '../api'

const labels = {
  ENVIRONMENT: '数据/环境问题',
  TOOL_CALL: '工具调用错误',
  INPUT_MISUNDERSTAND: '输入理解错误',
  PLANNING: '规划错误',
  OUTPUT_GENERATION: '输出生成错误',
  UNKNOWN: '未分类'
}

const taskId = ref('')
const results = ref([])
const metrics = ref(null)
const caseNames = ref({})
const loading = ref(false)
const savingId = ref(null)
const drafts = reactive({})

const pct = (v) => (v === null || v === undefined ? '—' : (Number(v) * 100).toFixed(1) + '%')
const caseName = (id) => caseNames.value[id] || `用例 #${id}`
const judgeOf = (row) => row.scores?.find((s) => s.scorer_type === 'LLM_JUDGE')?.score ?? null

function draftOf(row) {
  if (!drafts[row.result_id]) {
    drafts[row.result_id] = {
      human_score: judgeOf(row) === null ? 1 : null,
      human_failure_category: row.failure_category || null,
      remark: ''
    }
  }
  return drafts[row.result_id]
}

async function loadTask() {
  if (!taskId.value) {
    ElMessage.warning('请填写评测任务号')
    return
  }
  loading.value = true
  try {
    results.value = await evalApi.results(taskId.value)
    ElMessage.success(`载入 ${results.value.length} 条结果`)
  } finally {
    loading.value = false
  }
}

async function loadReport() {
  loading.value = true
  try {
    metrics.value = (await reviewApi.accuracy()).metrics
  } finally {
    loading.value = false
  }
}

async function submit(row) {
  const draft = draftOf(row)
  savingId.value = row.result_id
  try {
    await reviewApi.submit({
      result_id: row.result_id,
      human_score: draft.human_score,
      human_failure_category: draft.human_failure_category || null,
      reviewer: 'local',
      remark: draft.remark || null
    })
    ElMessage.success('已提交')
    await loadReport()
  } finally {
    savingId.value = null
  }
}

onMounted(async () => {
  const list = await testcaseApi.list({ enabled_only: true })
  caseNames.value = Object.fromEntries(list.map((c) => [c.testcase_id, c.name]))
  await loadReport()
})
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.stat-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 6px;
}
.stat-value {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 4px;
}
</style>
