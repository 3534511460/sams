<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
// 这里什么都不需要引，交给路由去控制页面
</script>

<style>
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
}
#app {
  height: 100%;
}
</style>