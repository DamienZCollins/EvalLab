import { createRouter, createWebHistory } from 'vue-router'
import TestCaseView from '../views/TestCaseView.vue'
import EvalTaskView from '../views/EvalTaskView.vue'

const routes = [
  { path: '/', redirect: '/testcase' },
  { path: '/testcase', name: 'testcase', component: TestCaseView, meta: { title: '用例管理' } },
  { path: '/eval', name: 'eval', component: EvalTaskView, meta: { title: '评测任务' } }
]

export default createRouter({ history: createWebHistory(), routes })
