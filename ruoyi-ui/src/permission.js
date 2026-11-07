import router from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'

NProgress.configure({ showSpinner: false })
const whiteList = ['/login', '/auth-redirect', '/bind', '/register']
const isAdmin = roles => Array.isArray(roles) && roles.includes('admin')

function destination(to) {
  if (store.state.user.passwordChangeRequired && to.path !== '/password') {
    return { path: '/password', replace: true }
  }
  if (!isAdmin(store.getters.roles) && !['/knowledge', '/password'].includes(to.path)) {
    return { path: '/knowledge', replace: true }
  }
  return null
}

router.beforeEach((to, from, next) => {
  NProgress.start()
  if (!getToken()) {
    if (whiteList.includes(to.path)) next()
    else next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
    NProgress.done()
    return
  }
  if (to.path === '/login') {
    next({ path: '/' })
    NProgress.done()
    return
  }
  if (store.getters.roles.length === 0) {
    store.dispatch('GetInfo').then(res => {
      const target = destination(to)
      if (target) return next(target)
      if (!isAdmin(res.roles)) return next({ ...to, replace: true })
      return store.dispatch('GenerateRoutes', { roles: res.roles }).then(routes => {
        router.addRoutes(routes)
        next({ ...to, replace: true })
      })
    }).catch(error => {
      store.dispatch('FedLogOut').then(() => {
        Message.error(error.message || '登录状态已失效')
        next({ path: '/login' })
      })
    })
    return
  }
  next(destination(to) || undefined)
})

router.afterEach(() => NProgress.done())
window.addEventListener('pageshow', event => {
  if (event.persisted) window.location.reload()
})
