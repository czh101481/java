<template>
  <div class="meter"><i :style="{ width: percent + '%', background: color }"></i></div>
  <div class="meter-label" :style="{ color: active ? 'var(--ink-2)' : 'var(--ink-3)' }">{{ text }}</div>
</template>

<script setup>
import { computed } from 'vue'
import { passwordScore, strengthOf } from '../composables/usePasswordStrength'

const props = defineProps({
  password: { type: String, default: '' }
})

const level = computed(() => strengthOf(passwordScore(props.password)))
const active = computed(() => props.password.length > 0)
const percent = computed(() => (active.value ? level.value.w : 0))
const color = computed(() => level.value.c)
const text = computed(() => level.value.t)
</script>

<style scoped>
.meter{height:5px;border-radius:99px;background:rgba(150,170,220,.2);overflow:hidden;margin:-6px 2px 12px}
.meter > i{display:block;height:100%;width:0;border-radius:99px;transition:width .35s var(--ease), background .35s var(--ease)}
.meter-label{font-size:11px;color:var(--ink-3);margin:-4px 2px 12px}
</style>
