<template>
  <div
    class="sidebar-logo-container"
    :class="{ collapse: collapse }"
  >
    <router-link class="sidebar-logo-link" to="/">
      <div class="project-mark">
        <i class="el-icon-connection"></i>
      </div>

      <div
        v-if="!collapse"
        class="project-title"
      >
        向量库访问控制系统
      </div>
    </router-link>
  </div>
</template>

<script>
export default {
  name: 'SidebarLogo',

  props: {
    collapse: {
      type: Boolean,
      required: true
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebar-logo-container {
  position: relative;
  width: 100%;
  height: 64px;
  overflow: hidden;
  background: #17283d;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.sidebar-logo-link {
  width: 100%;
  height: 64px;
  padding: 0 16px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  text-decoration: none;
}

.project-mark {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 8px;
  background: #2f82cf;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.project-mark i {
  font-size: 18px;
}

.project-title {
  min-width: 0;
  margin-left: 11px;
  color: #f1f5f9;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  letter-spacing: 0.2px;
}

.sidebar-logo-container.collapse {
  .sidebar-logo-link {
    padding: 0;
    justify-content: center;
  }

  .project-mark {
    width: 32px;
    height: 32px;
  }
}
</style>
