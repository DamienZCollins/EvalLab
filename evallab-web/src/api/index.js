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

export const reviewApi = {
  submit: (body) => client.post('/manual-review', body),
  accuracy: () => client.get('/manual-review/accuracy')
}
