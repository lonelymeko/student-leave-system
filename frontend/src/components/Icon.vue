<script setup>
const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 18 }
})

// Inline SVG paths, line style (Fluent/SF Symbols like), stroke=currentColor
const paths = {
  home: 'M3 10.5 12 3l9 7.5M5 9.5V20a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1V9.5',
  list: 'M8 6h13M8 12h13M8 18h13M3.5 6h.01M3.5 12h.01M3.5 18h.01',
  plus: 'M12 5v14M5 12h14',
  check: 'M4.5 12.5 10 18 19.5 6.5',
  x: 'M6 6l12 12M18 6 6 18',
  user: 'M12 12a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9Zm-8 9c0-3.6 3.6-6 8-6s8 2.4 8 6',
  users: 'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-7 9c0-3.3 3.1-5.5 7-5.5s7 2.2 7 5.5M16.5 3.3a4 4 0 0 1 0 7.4M18.5 14.7c2.1.7 3.5 2.1 3.5 4.3',
  chart: 'M4 20V10M10 20V4M16 20v-7M21 20H3',
  logout: 'M15 4h4a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-4M10 8l-4 4 4 4M6 12h11',
  sparkle: 'M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9L12 3ZM19 15l.9 2.1L22 18l-2.1.9L19 21l-.9-2.1L16 18l2.1-.9L19 15Z',
  clock: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Zm0-14v5l3.5 2',
  calendar: 'M4 6.5A1.5 1.5 0 0 1 5.5 5h13A1.5 1.5 0 0 1 20 6.5v13a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 19.5v-13ZM4 9.5h16M8 3v4M16 3v4',
  'chevron-right': 'M9 5l7 7-7 7',
  'chevron-left': 'M15 5l-7 7 7 7',
  'chevron-down': 'M5 9l7 7 7-7',
  search: 'M10.5 18a7.5 7.5 0 1 0 0-15 7.5 7.5 0 0 0 0 15Zm10.5 3-5.2-5.2',
  edit: 'M4 20h4L20 8a2.1 2.1 0 0 0-3-3L5 17l-1 4ZM14.5 6.5l3 3',
  trash: 'M4 7h16M9.5 7V4.5A1.5 1.5 0 0 1 11 3h2a1.5 1.5 0 0 1 1.5 1.5V7M6.5 7l1 12.5a1.5 1.5 0 0 0 1.5 1.4h6a1.5 1.5 0 0 0 1.5-1.4l1-12.5M10 11v6M14 11v6',
  key: 'M14 13.5a5.25 5.25 0 1 0-5-3.7L3 15.8V19h3.2l1-1.6h2.2l1.1-2 1.4-1.4c.6.3 1.3.5 2.1.5Zm1.5-6a1 1 0 1 1 0-2 1 1 0 0 1 0 2Z',
  send: 'M21 3 3.6 9.9c-.8.3-.8 1.5.1 1.7l7 1.7 1.7 7c.2.9 1.4.9 1.7.1L21 3ZM21 3l-10.3 10.3',
  message: 'M12 20.5c5 0 9-3.6 9-8s-4-8-9-8-9 3.6-9 8c0 2 .8 3.7 2.1 5.1L4 21.5l4.2-1.6c1.2.4 2.4.6 3.8.6Z',
  doc: 'M7 3h7l5 5v12a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Zm7 0v5h5M9.5 13h5M9.5 16.5h5',
  inbox: 'M4 13.5 6.5 5A1.5 1.5 0 0 1 8 4h8a1.5 1.5 0 0 1 1.5 1L20 13.5V19a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1v-5.5Zm0 0h4.5l1.2 2.5h4.6l1.2-2.5H20',
  history: 'M4.5 5.5V9H8M4 12a8 8 0 1 1 2.3 5.6M12 7.5V12l3 2',
  warning: 'M12 4 2.7 19.5a1 1 0 0 0 .9 1.5h16.8a1 1 0 0 0 .9-1.5L12 4Zm0 6v4.5M12 17.8h.01',
  phone: 'M6 3h3l1.5 4.5L8.5 9a12 12 0 0 0 6.5 6.5l1.5-2L21 15v3a2 2 0 0 1-2 2A16 16 0 0 1 4 5a2 2 0 0 1 2-2Z',
  pin: 'M12 21s7-6.1 7-11.5A7 7 0 0 0 5 9.5C5 14.9 12 21 12 21Zm0-9a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5Z',
  lock: 'M7 10.5V8a5 5 0 0 1 10 0v2.5M5.5 10.5h13a1 1 0 0 1 1 1V20a1 1 0 0 1-1 1h-13a1 1 0 0 1-1-1v-8.5a1 1 0 0 1 1-1ZM12 14.5v3',
  eye: 'M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Zm9.5 3a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z',
  refresh: 'M19.5 9A8 8 0 0 0 5 7.5L4 9M4.5 4.5V9H9M4.5 15a8 8 0 0 0 14.5 1.5l1-1.5M19.5 19.5V15H15',
  undo: 'M8 5 4 9l4 4M4 9h10a6 6 0 0 1 0 12h-4',
  minus: 'M5 12h14'
}
</script>

<template>
  <svg
    :width="size" :height="size" viewBox="0 0 24 24" fill="none"
    stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"
    aria-hidden="true" style="flex-shrink:0"
  >
    <path :d="paths[name] || paths.doc" />
  </svg>
</template>
