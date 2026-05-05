import { createRouter, createWebHistory } from 'vue-router'
import LandingView from '../views/LandingView.vue'
import HomeView from '../views/HomeView.vue'
import LoginView from '../components/LoginView.vue'
import { getAuthToken } from '../services/api'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: LandingView
    },
    {
      path: '/app',
      name: 'app',
      component: HomeView,
      meta: { requiresAuth: true }
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView
    }
  ]
})

router.beforeEach((to) => {
  const hasToken = Boolean(getAuthToken())
  if (to.meta.requiresAuth && !hasToken) {
    return { name: 'login' }
  }
  if (to.name === 'login' && hasToken) {
    return { name: 'app' }
  }
  return true
})

export default router
