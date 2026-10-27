<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-select v-model="filters.groupId" placeholder="全部分组" clearable style="width: 160px" @change="load">
        <el-option v-for="g in groups" :key="g.group_id" :label="g.group_name" :value="g.group_id" />
      </el-select>
      <el-input v-model="filters.tag" placeholder="按标签筛选" clearable style="width: 180px" @keyup.enter="load" />
      <el-checkbox v-model="filters.enabledOnly" @change="load">只看启用</el-checkbox>
      <el-button type="primary" @click="openCreate">新建用例</el-button>
      <el-button :loading="loading" @click="load">刷新</el-button>
      <span class="muted">共 {{ cases.length }} 条</span>
    </div>

    <el-table :data="cases" v-loading="loading" stripe>
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="input" label="输入" min-width="220" show-overflow-tooltip />
      <el-table-column label="判定依据" min-width="180">
        <template #default="{ row }">
          <el-tag v-if="row.match_type !== 'NONE'" size="small" type="success">{{ row.match_type }}</el-tag>
          <el-tag v-if="row.expected_tools?.length" size="small" type="warning" style="margin-left: 4px">
            工具 ×{{ row.expected_tools.length }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="故障注入" width="150">
        <template #default="{ row }">
          <span v-if="row.fault_injection" class="muted">{{ row.fault_injection.type }}</span>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="version" label="版本" width="70" />
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

  <el-dialog v-model="dialogVisible" :title="editing ? '编辑用例' : '新建用例'" width="640px">
    <el-form :model="form" label-width="110px">
      <el-form-item label="名称" required>
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="输入" required>
        <el-input v-model="form.input" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="分组">
        <el-select v-model="form.group_id" placeholder="不分组" clearable style="width: 100%">
          <el-option v-for="g in groups" :key="g.group_id" :label="g.group_name" :value="g.group_id" />
        </el-select>
      </el-form-item>
      <el-form-item label="期望匹配方式">
        <el-select v-model="form.match_type" style="width: 100%">
          <el-option label="精确匹配 EXACT" value="EXACT" />
          <el-option label="正则匹配 REGEX" value="REGEX" />
          <el-option label="JSON Schema" value="JSON_SCHEMA" />
          <el-option label="不校验输出 NONE" value="NONE" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.match_type !== 'NONE'" label="期望输出">
        <el-input v-model="form.expected_output" type="textarea" :rows="4"
                  :placeholder="placeholderForMatchType" />
      </el-form-item>
      <el-form-item label="期望工具序列">
        <el-input v-model="toolsText" placeholder="逗号分隔，如 web_search,extract,summarize；留空不校验" />
      </el-form-item>
      <el-form-item label="故障注入">
        <el-select v-model="form.fault_injection.type" placeholder="不注入" clearable style="width: 100%">
          <el-option v-for="t in faultTypes" :key="t" :label="t" :value="t" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.fault_injection.type" label="目标 span 序号">
        <el-input v-model="form.fault_injection.target_span" placeholder="留空用默认位置" />
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
import { testcaseApi } from '../api'

const faultTypes = ['TOOL_ERROR', 'STEP_MISSING', 'OUTPUT_FORMAT_ERROR', 'TIMEOUT', 'INPUT_MISUNDERSTAND']

const cases = ref([])
const groups = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editing = ref(null)
const toolsText = ref('')
const filters = ref({ groupId: null, tag: '', enabledOnly: false })

const emptyForm = () => ({
  group_id: null,
  name: '',
  input: '',
  match_type: 'EXACT',
  expected_output: '',
  expected_tools: [],
  fault_injection: { type: null, target_span: null },
  tags: ['demo'],
  enabled: true
})
const form = ref(emptyForm())

const placeholderForMatchType = computed(() => {
  if (form.value.match_type === 'REGEX') return 'Java 全串匹配正则，如 \\{\\"answer\\": \\".+\\"}'
  if (form.value.match_type === 'JSON_SCHEMA') return 'JSON Schema 文档，输出会先解析为 JSON 再校验'
  return '与 Agent 最终输出逐字符一致（比较前 trim）'
})

async function load() {
  loading.value = true
  try {
    cases.value = await testcaseApi.list({
      group_id: filters.value.groupId || undefined,
      tag: filters.value.tag || undefined,
      enabled_only: filters.value.enabledOnly
    })
  } finally {
    loading.value = false
  }
}

async function loadGroups() {
  groups.value = await testcaseApi.groups()
}

function openCreate() {
  editing.value = null
  form.value = emptyForm()
  toolsText.value = ''
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  form.value = {
    ...emptyForm(),
    ...row,
    fault_injection: { type: row.fault_injection?.type || null, target_span: row.fault_injection?.target_span || null }
  }
  toolsText.value = (row.expected_tools || []).join(',')
  dialogVisible.value = true
}

async function save() {
  const payload = {
    ...form.value,
    expected_tools: toolsText.value.split(',').map((t) => t.trim()).filter(Boolean),
    fault_injection: form.value.fault_injection.type ? form.value.fault_injection : null
  }
  if (!payload.fault_injection) {
    delete payload.fault_injection
  }
  saving.value = true
  try {
    if (editing.value) {
      await testcaseApi.update(editing.value.testcase_id, payload)
      ElMessage.success('已更新，版本号 +1')
    } else {
      await testcaseApi.create(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除用例「${row.name}」？`, '删除确认', { type: 'warning' })
  await testcaseApi.remove(row.testcase_id)
  ElMessage.success('已删除')
  await load()
}

onMounted(async () => {
  await loadGroups()
  await load()
})
</script>
