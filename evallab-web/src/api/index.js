import client from './client'

export const testcaseApi = {
  list: (params) => client.get('/testcase', { params }),
  get: (id) => client.get(`/testcase/${id}`),
  create: (body) => client.post('/testcase', body),
  update: (id, body) => client.put(`/testcase/${id}`, body),
  remove: (id) => client.delete(`/testcase/${id}`),
  groups: () => client.get('/testcase/group'),
  createGroup: (groupName) => client.post('/testcase/group', { group_name: groupName })
}

export const evalApi = {
  createTask: (body) => client.post('/eval/task', body),
  getTask: (id) => client.get(`/eval/task/${id}`),
  results: (id) => client.get(`/eval/task/${id}/result`)
}

export const traceApi = {
  get: (traceId) => client.get(`/trace/${traceId}`)
}

export const baselineApi = {
  list: () => client.get('/baseline'),
  create: (body) => client.post('/baseline', body),
  // 422 也照常返回响应体，由页面展示门禁结论，不再弹全局错误
  gate: (body) =>
    client.post('/eval/gate', body, { validateStatus: (status) => status === 200 || status === 422 })
}

export const redteamApi = {
  list: (params) => client.get('/redteam', { params }),
  create: (body) => client.post('/redteam', body),
  update: (id, body) => client.put(`/redteam/${id}`, body),
  remove: (id) => client.delete(`/redteam/${id}`),
  report: (taskId) => client.get(`/eval/task/${taskId}/redteam-report`)
}

export const reviewApi = {
  submit: (body) => client.post('/manual-review', body),
  accuracy: () => client.get('/manual-review/accuracy')
}
