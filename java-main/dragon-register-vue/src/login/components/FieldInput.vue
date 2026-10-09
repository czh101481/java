<template>
  <!-- 浮标签输入框：placeholder=" " + :not(:placeholder-shown) 实现浮标签 -->
  <!-- 与注册页 src/register/components/FieldInput.vue 保持同构，风格统一 -->
  <div class="field" :class="[state, { shake: shaking }]">
    <input
      :id="id"
      :type="inputType"
      :value="modelValue"
      :autocomplete="autocomplete"
      placeholder=" "
      @input="onInput"
    />
    <label :for="id">{{ label }}</label>
    <button
      v-if="toggleable"
      class="toggle"
      type="button"
      :aria-label="revealed ? '隐藏密码' : '显示密码'"
      @click="revealed = !revealed"
    >
      <EyeIcon :off="revealed" />
    </button>
    <span v-if="hint" class="hint" :class="{ show: hintVisible }">{{ hint }}</span>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import EyeIcon from './icons/EyeIcon.vue'

const props = defineProps({
  id: { type: String, required: true },
  modelValue: { type: String, default: '' },
  label: { type: String, required: true },
  type: { type: String, default: 'text' },
  autocomplete: { type: String, default: 'off' },
  /** '' | 'bad' | 'good' */
  state: { type: String, default: '' },
  hint: { type: String, default: '' },
  hintVisible: { type: Boolean, default: false },
  /** 密码框显示「眼睛」切换按钮 */
  toggleable: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'input'])

const revealed = ref(false)
const shaking = ref(false)

const inputType = computed(() => {
  if (!props.toggleable) return props.type
  return revealed.value ? 'text' : 'password'
})

function onInput(event) {
  emit('update:modelValue', event.target.value)
  emit('input', event)
}

/** 供父组件调用：校验失败时抖动该字段 */
function shake() {
  shaking.value = true
  setTimeout(() => (shaking.value = false), 420)
}

defineExpose({ shake, revealed })
</script>

<style scoped>
.field{position:relative;margin-bottom:14px}
.field input{
  width:100%;padding:18px 44px 8px 16px;font-size:15px;color:var(--ink);
  background:rgba(8,12,22,.45);
  border:1px solid rgba(150,170,220,.25);border-radius:14px;outline:none;
  transition:border-color .25s var(--ease), box-shadow .25s var(--ease), background .25s var(--ease);
}
.field label{
  position:absolute;left:16px;top:15px;font-size:15px;color:var(--ink-3);pointer-events:none;
  transition:transform .2s var(--ease), color .2s var(--ease), font-size .2s var(--ease);
}
.field input:focus,.field input:not(:placeholder-shown){
  background:rgba(8,12,22,.7);
  border-color:var(--accent);
  box-shadow:0 0 0 4px rgba(232,184,75,.16);
}
.field input:focus+label,.field input:not(:placeholder-shown)+label{transform:translateY(-10px);font-size:11px;color:var(--ink-2)}
.field input:focus+label{color:var(--accent)}

.field.bad input{border-color:var(--err);box-shadow:0 0 0 4px rgba(255,107,107,.14)}
.field.good input{border-color:var(--ok);box-shadow:0 0 0 4px rgba(91,231,169,.14)}

.toggle{
  position:absolute;right:12px;top:14px;width:26px;height:26px;
  border:none;background:none;cursor:pointer;color:var(--ink-3);
  display:grid;place-items:center;
  transition:color .2s var(--ease);
}
.toggle:hover{color:var(--accent)}
.toggle svg{width:19px;height:19px}

.hint{
  font-size:11.5px;margin:-8px 2px 12px;min-height:14px;
  color:var(--err);opacity:0;
  transition:opacity .2s var(--ease);
}
.hint.show{opacity:1}
</style>
