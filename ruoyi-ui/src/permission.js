import router from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'

NProgress.configure({ showSpinner: false })

const whiteList = [
  '/login',
  '/auth-redirect',
  '/bind',
  '/register'
]

function isAdminUser(roles) {
  return Array.isArray(roles) &&
    roles.indexOf('admin') !== -1
}

router.beforeEach((to, from, next) => {
  NProgress.start()

  if (getToken()) {
    if (to.path === '/login') {
      /*
       * 暂时先回到根路径。
       * 根路径随后会完成 GetInfo，
       * 再根据角色决定进入管理后台还是知识库门户。
       */
      next({ path: '/' })
      NProgress.done()
      return
    }

    if (store.getters.roles.length === 0) {
      store.dispatch('GetInfo')
        .then(res => {
          const roles = res.roles || []

          /*
           * 普通用户：
           * 不生成后台菜单路由，
           * 无论访问什么后台地址都进入知识库门户。
           */
          if (!isAdminUser(roles)) {
            if (to.path === '/knowledge') {
              next({
                ...to,
                replace: true
              })
            } else {
              next({
                path: '/knowledge',
                replace: true
              })
            }

            return
          }

          /*
           * 管理员保持原有若依后台路由生成逻辑。
           */
          store.dispatch(
            'GenerateRoutes',
            { roles }
          ).then(accessRoutes => {
            router.addRoutes(accessRoutes)

            next({
              ...to,
              replace: true
            })
          })
        })
        .catch(err => {
          store.dispatch('FedLogOut')
            .then(() => {
              Message.error(err)
              next({ path: '/login' })
            })
        })

      return
    }

    /*
     * 用户信息已经加载完成后，
     * 普通用户仍然不能手动输入后台 URL。
     */
    if (
      !isAdminUser(store.getters.roles) &&
      to.path !== '/knowledge'
    ) {
      next({
        path: '/knowledge',
        replace: true
      })

      return
    }

    next()
  } else {
    if (whiteList.indexOf(to.path) !== -1) {
      next()
    } else {
      next(
        `/login?redirect=${to.fullPath}`
      )

      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
