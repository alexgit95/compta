const CACHE_NAME = 'budget-app-v2';
const PRECACHE_URLS = [
  '/',
  '/css/app.css',
  '/manifest.webmanifest',
  '/icons/icon-192.svg',
  '/icons/icon-512.svg'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(PRECACHE_URLS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', event => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') return;
  event.respondWith(
    caches.match(event.request).then(cached => {
      if (cached) return cached;
      return fetch(event.request).catch(() => new Response('', { status: 503 }));
    })
  );
});
