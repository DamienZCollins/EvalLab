<template>
  <el-card shadow="never" class="block">
    <div class="toolbar">
      <el-select v-model="filters.attackType" placeholder="全部攻击类型" clearable style="width: 200px" @change="load">
        <el-option v-for="t in ATTACK_TYPES" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
      <el-button type="primary" @click="openCreate">新建红队用例</el-button>
      <el-button :loading="loading" @click="load">刷新</el-button>
      <span class="muted">共 {{ cases.length }} 条 · 输出命中禁用标记即判定被攻破</span>
    </div>

    <el-table :data="cases" v-loading="loading" stripe>
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column label="攻击类型" width="150">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.attack_type)" size="small">{{ typeLabel(row.attack_type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="attack_payload" label="攻击载荷" min-width="260" show-overflow-tooltip />
      <el-table-column label="禁用标记" min-width="200">
        <template #default="{ row }">
          <el-tag v-for="m in row.forbidden_markers" :key="m" size="small" type="danger" class="marker">{{ m }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-card shadow="never" class="block">
    <div class="panel-title">红队评测报告</div>
    <div class="toolbar">
      <el-input v-model="reportTaskId" placeholder="评测任务 ID（按 redteam 分组发起）" style="width: 320px" />
      <el-button type="primary" :loading="loadingReport" @click="loadReport">查看报告</el-button>
      <span class="muted">拦截率 = 防御成功数 / 红队用例数</span>
    </div>

    <template v-if="report">
      <div class="stats">
        <div class="stat">
          <div class="stat-label">拦截率</div>
          <div class="stat-value" :class="{ breached: rate < 1 }">{{ percent(report.interception_rate) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">防御成功 / 总数</div>
          <div class="stat-value">{{ report.intercepted_attacks }} / {{ report.total_attacks }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">被攻破</div>
          <div class="stat-value">{{ report.breached_attacks }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">被测 Agent</div>
          <div class="stat-value">{{ report.agent_name }}</div>
        </div>
      </div>

      <el-alert v-if="!report.total_attacks" type="info" :closable="false" show-icon style="margin: 16px 0"
                title="该任务里没有红队用例：发起评测时请把分组选为 redteam" />

      <el-table v-if="Object.keys(report.by_type || {}).length" :data="typeRows" stripe style="margin-top: 16px">
        <el-table-column label="攻击类型" width="200">
          <template #default="{ row }">{{ typeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="total_attacks" label="攻击数" width="100" />
        <el-table-column prop="intercepted_attacks" label="防御成功" width="110" />
        <el-table-column prop="breached_attacks" label="被攻破" width="100" />
        <el-table-column label="拦截率" width="120">
          <template #default="{ row }">{{ percent(row.interception_rate) }}</template>
        </el-table-column>
      </el-table>

      <el-table v-if="report.attacks?.length" :data="report.attacks" stripe style="margin-top: 16px">
        <el-table-column prop="name" label="用例" min-width="220" />
        <el-table-column label="类型" width="150">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.attack_type)" size="small">{{ typeLabel(row.attack_type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结论" width="110">
          <template #default="{ row }">
            <el-tag :type="row.intercepted ? 'success' : 'danger'" size="small">
              {{ row.intercepted ? '已拦截' : '被攻破' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{ muted: !row.failure_reason }">{{ row.failure_reason || '输出未命中任何禁用标记' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Trace" width="140">
          <template #default="{ row }">
            <router-link v-if="row.trace_id" :to="`/trace/${row.trace_id}`" class="mono">
              {{ row.trace_id.slice(0, 8) }}…
            </router-link>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="editing ? '编辑红队用例' : '新建红队用例'" width="620px">
    <el-form :model="form" label-width="110px">
      <el-form-item label="名称" required>
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="攻击类型" required>
        <el-select v-model="form.attack_type" style="width: 100%">
          <el-option v-for="t in ATTACK_TYPES" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="攻击载荷" required>
        <el-input v-model="form.attack_payload" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item label="期望防御" required>
        <el-input v-model="form.expected_defense" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="禁用标记" required>
        <el-input v-model="markersText" placeholder="逗号分隔；Agent 输出出现任一标记即判定被攻破" />
      </el-form-item>
      <el-form-item label="启用">
        <el-switch v-model="form.enabled" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { redteamApi } from '../api'

const ATTACK_TYPES = [
  { value: 'PROMPT_INJECTION', label: '提示注入 PROMPT_INJECTION' },
  { value: 'JAILBREAK', label: '越狱 JAILBREAK' },
  { value: 'PII_LEAK', label: '隐私泄露 PII_LEAK' }
]

const cases = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editing = ref(null)
const markersText = ref('')
const filters = ref({ attackType: null })
const reportTaskId = ref('')
const report = ref(null)
const loadingReport = ref(false)

const emptyForm = () => ({
  name: '',
  attack_type: 'PROMPT_INJECTION',
  attack_payload: '',
  expected_defense: '',
  forbidden_markers: [],
  enabled: true
})
const form = ref(emptyForm())

const typeLabel = (value) => (ATTACK_TYPES.find((t) => t.value === value)?.label.split(' ')[0]) || value
const typeTag = (value) => ({ PROMPT_INJECTION: 'warning', JAILBREAK: 'danger', PII_LEAK: 'info' })[value] || 'info'
const percent = (v) => (v === null || v === undefined ? '—' : (Number(v) * 100).toFixed(1) + '%')
const rate = computed(() => Number(report.value?.interception_rate ?? 0))

const typeRows = computed(() =>
  Object.entries(report.value?.by_type || {}).map(([type, stat]) => ({ type, ...stat }))
)

async function load() {
  loading.value = true
  try {
    cases.value = await redteamApi.list({ attack_type: filters.value.attackType || undefined })
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.value = emptyForm()
  markersText.value = ''
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  form.value = { ...emptyForm(), ...row }
  markersText.value = (row.forbidden_markers || []).join(',')
  dialogVisible.value = true
}

async function save() {
  const payload = {
    ...form.value,
    forbidden_markers: markersText.value.split(',').map((m) => m.trim()).filter(Boolean)
  }
  saving.value = true
  try {
    if (editing.value) {
      await redteamApi.update(editing.value.redteam_case_id, payload)
      ElMessage.success('已更新，底层用例版本号 +1')
    } else {
      await redteamApi.create(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除红队用例「${row.name}」？底层评测用例会一并删除。`,
    '删除确认', { type: 'warning' })
  await redteamApi.remove(row.redteam_case_id)
  ElMessage.success('已删除')
  await load()
}

async function loadReport() {
  if (!reportTaskId.value) {
    ElMessage.warning('请填写评测任务 ID')
    return
  }
  loadingReport.value = true
  try {
    report.value = await redteamApi.report(reportTaskId.value)
  } finally {
    loadingReport.value = false
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
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.stats {
  display: flex;
  gap: 40px;
  flex-wrap: wrap;
  margin-top: 8px;
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
.stat-value.breached {
  color: #e6a23c;
}
.marker {
  margin: 0 4px 4px 0;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}
</style>
