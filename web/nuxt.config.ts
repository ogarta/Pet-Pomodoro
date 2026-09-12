// PetPomodoro — Nuxt 4, offline core. PWA qua @vite-pwa/nuxt (SW chỉ build, không bật ở dev).
// Ghi chú: Nuxt 4.4.5 có bug dev với `ssr:false` ("No entry found in rollupOptions.input")
// nên giữ SSR mặc định; mọi thứ chạm browser (localStorage/timer) đã guard import.meta.client.
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: false },
  modules: [['@vite-pwa/nuxt', {
    registerType: 'autoUpdate',
    includeAssets: ['pets/**', 'favicon-32.png'],
    manifest: {
      id: '/',
      name: 'Pet Pomodoro',
      short_name: 'PetPomodoro',
      lang: 'vi',
      description: 'Pomodoro nuôi pet tiến hoá — tập trung cùng bạn đồng hành pixel của bạn',
      theme_color: '#F2E9D8',
      background_color: '#F2E9D8',
      display: 'standalone',
      start_url: '/',
      icons: [
        { src: '/icon-192.png', sizes: '192x192', type: 'image/png' },
        { src: '/icon-512.png', sizes: '512x512', type: 'image/png' },
        { src: '/maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
      ],
    },
    workbox: { globPatterns: ['**/*.{js,css,html,png,svg,woff2}'] },
    devOptions: { enabled: false },
  }]],
  app: {
    head: {
      title: 'PetPomodoro — Pomodoro nuôi pet',
      htmlAttrs: { lang: 'vi' },
      meta: [
        { name: 'viewport', content: 'width=device-width, initial-scale=1, viewport-fit=cover' },
        { name: 'description', content: 'Pomodoro nuôi pet tiến hoá — tập trung cùng bạn đồng hành pixel của bạn' },
        { name: 'theme-color', content: '#F2E9D8' },
      ],
      link: [
        { rel: 'icon', type: 'image/png', href: '/favicon-32.png' },
        { rel: 'manifest', href: '/manifest.webmanifest' },
        { rel: 'apple-touch-icon', href: '/icon-192.png' },
        { rel: 'preconnect', href: 'https://fonts.googleapis.com' },
        { rel: 'preconnect', href: 'https://fonts.gstatic.com', crossorigin: '' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Bungee&family=DM+Serif+Display:ital@0;1&family=Space+Mono:ital,wght@0,400;0,700;1,400&display=swap' },
      ],
    },
  },
  css: ['~/assets/css/main.css'],
  typescript: { strict: true },
})
