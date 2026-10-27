import axios from 'axios'
import { ElMessage } from 'element-plus'

const client = axios.create({ baseURL: '/api', timeout: 15000 })

// 后端统一返回 {code, message, data}：code 非 0 即为业务错误，统一弹提示并 reject
client.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    // 门禁接口直接返回业务对象，不套 {code, message, data}
    return body && typeof body.code === 'number' ? body.data : body
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络错误'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default client
