import { createRouter, createWebHistory } from 'vue-router'
import TestCaseView from '../views/TestCaseView.vue'
import EvalTaskView from '../views/EvalTaskView.vue'
import ManualReviewView from '../views/ManualReviewView.vue'
import BaselineView from '../views/BaselineView.vue'
import RedteamView from '../views/RedteamView.vue'
import TraceDetailView from '../views/TraceDetailView.vue'

const routes = [
  { path: '/', redirect: '/testcase' },
  { path: '/testcase', name: 'testcase', component: TestCaseView, meta: { title: '用例管理' } },
  { path: '/eval', name: 'eval', component: EvalTaskView, meta: { title: '评测任务' } },
  { path: '/trace/:traceId', name: 'trace', component: TraceDetailView, meta: { title: 'Trace 详情' } },
  { path: '/baseline', name: 'baseline', component: BaselineView, meta: { title: '基线与门禁' } },
  { path: '/redteam', name: 'redteam', component: RedteamView, meta: { title: '红队评测' } },
  { path: '/review', name: 'review', component: ManualReviewView, meta: { title: '人工抽检' } }
]

export default createRouter({ history: createWebHistory(), routes })
