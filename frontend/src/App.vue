<template>
  <div id="app">
    <el-config-provider>
      <router-view />
    </el-config-provider>
  </div>
</template>

<script setup>
// 这里可以添加全局逻辑
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family:
    'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial,
    sans-serif;
  background-color: #f5f7fa;
  /* [UI美化] 默认文字色与行高全站统一，标题/辅助文字由各组件按层级覆盖 */
  color: #303133;
  line-height: 1.5;
}

#app {
  min-height: 100vh;
}
</style>
