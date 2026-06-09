<template>
  <div class="global-preview-overlay"
    @click="$emit('close')"
    @touchstart.passive="onTouchStart"
    @touchend="onTouchEnd"
  >
    <img v-if="currentItem?.type === 'image'"
      :src="currentItem.url"
      class="preview-media"
      @click.stop />

    <video v-else-if="currentItem?.type === 'video'"
      :src="currentItem.url"
      :poster="currentItem.cover"
      class="preview-media"
      controls
      autoplay
      playsinline
      @click.stop />

    <div class="preview-counter">{{ currentIndex + 1 }} / {{ mediaList.length }}</div>

    <button v-if="currentIndex > 0" class="preview-arrow left" @click.stop="prev">‹</button>
    <button v-if="currentIndex < mediaList.length - 1" class="preview-arrow right" @click.stop="next">›</button>
  </div>
</template>

<script setup lang="ts">
import { computed, watch, onMounted, onUnmounted } from 'vue'

interface MediaItem {
  type: 'image' | 'video'
  url: string
  cover?: string
}

const props = defineProps<{
  mediaList: MediaItem[]
  currentIndex: number
}>()

const emit = defineEmits<{
  close: []
  'update:index': [index: number]
}>()

let touchStartX = 0

const currentItem = computed(() => props.mediaList[props.currentIndex])

const prev = () => {
  if (props.currentIndex > 0) {
    emit('update:index', props.currentIndex - 1)
  }
}

const next = () => {
  if (props.currentIndex < props.mediaList.length - 1) {
    emit('update:index', props.currentIndex + 1)
  }
}

const onTouchStart = (e: TouchEvent) => {
  const touch = e.touches[0]
  if (!touch) return
  touchStartX = touch.clientX
}

const onTouchEnd = (e: TouchEvent) => {
  const touch = e.changedTouches[0]
  if (!touch) return
  const diff = touchStartX - touch.clientX

  if (Math.abs(diff) > 50) {
    if (diff > 0 && props.currentIndex < props.mediaList.length - 1) {
      emit('update:index', props.currentIndex + 1)
    } else if (diff < 0 && props.currentIndex > 0) {
      emit('update:index', props.currentIndex - 1)
    }
  }
}

onMounted(() => {
  document.body.style.overflow = 'hidden'
  document.body.style.overscrollBehavior = 'none'
})

onUnmounted(() => {
  document.body.style.overflow = ''
  document.body.style.overscrollBehavior = ''
})
</script>

<style scoped>
.global-preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.95);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-media {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  transition: transform 0.3s;
}

.preview-counter {
  position: absolute;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  color: white;
  font-size: 14px;
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
}

.preview-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  color: white;
  font-size: 40px;
  padding: 20px;
  cursor: pointer;
  text-shadow: 0 1px 3px rgba(0,0,0,0.5);
  opacity: 0.8;
  transition: opacity 0.2s;
}

.preview-arrow:hover {
  opacity: 1;
}

.preview-arrow.left {
  left: 0;
}

.preview-arrow.right {
  right: 0;
}
</style>
