<template>
  <el-container class="layout">
    <el-header class="header">
      <span class="brand">EvalLab</span>
      <el-menu mode="horizontal" :default-active="$route.path" router :ellipsis="false" class="menu">
        <el-menu-item index="/testcase">用例管理</el-menu-item>
        <el-menu-item index="/eval">评测任务</el-menu-item>
        <el-menu-item index="/baseline">基线与门禁</el-menu-item>
        <el-menu-item index="/redteam">红队评测</el-menu-item>
        <el-menu-item index="/review">人工抽检</el-menu-item>
      </el-menu>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<style>
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: #f5f7fa;
  color: #303133;
}
.layout {
  min-height: 100vh;
}
.header {
  display: flex;
  align-items: center;
  gap: 32px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 24px;
}
.brand {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.menu {
  border-bottom: none;
}
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.muted {
  color: #909399;
  font-size: 13px;
}
</style>
