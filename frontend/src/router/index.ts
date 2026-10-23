import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/Login.vue'),
      meta: { public: true },
    },
    {
      path: '/s/:shareCode',
      name: 'Answer',
      component: () => import('@/views/public/AnswerPage.vue'),
      meta: { public: true },
    },
    {
      path: '/s/:shareCode/done',
      name: 'AnswerDone',
      component: () => import('@/views/public/AnswerDone.vue'),
      meta: { public: true },
    },
    {
      path: '/admin',
      component: () => import('@/components/layout/AdminLayout.vue'),
      redirect: '/admin/surveys',
      children: [
        {
          path: 'surveys',
          name: 'SurveyList',
          component: () => import('@/views/admin/SurveyList.vue'),
        },
        {
            path: 'surveys/:id/edit',
            name: 'SurveyEdit',
            component: () => import('@/views/admin/SurveyEdit.vue'),
        },
        {
            path: 'surveys/:id/preview',
            name: 'SurveyPreview',
            component: () => import('@/views/admin/SurveyPreview.vue'),
        },
        {
            path: 'surveys/:id/result',
            name: 'SurveyResult',
            component: () => import('@/views/admin/SurveyResult.vue'),
      },
      ],
    },
    {
      path: '/',
      redirect: '/admin/surveys',
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/admin/surveys',
    },
  ],
})

// 全局守卫：/admin/** 需要登录
router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    // 已登录访问 /login，跳回列表
    if (auth.isLoggedIn && to.path === '/login') {
      return { path: '/admin/surveys' }
    }
    return true
  }

  if (!auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  return true
})

export default router