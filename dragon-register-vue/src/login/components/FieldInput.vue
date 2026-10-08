<template>
  <div class="field">
    <input
      :id="id"
      :type="type"
      :value="modelValue"
      :autocomplete="autocomplete"
      placeholder=" "
      @input="$emit('update:modelValue', $event.target.value)"
    />
    <label :for="id">{{ label }}</label>
  </div>
</template>

<script setup>
/**
 * 浮标签输入框（Sakura Pass 主题，样式自 login-anime.html 原样平移）
 * 注意：input 必须紧邻 label，否则 `input:not(:placeholder-shown)+label` 浮起效果失效
 */
defineProps({
  id: { type: String, required: true },
  modelValue: { type: String, default: '' },
  label: { type: String, required: true },
  type: { type: String, default: 'text' },
  autocomplete: { type: String, default: 'off' }
})
defineEmits(['update:modelValue'])
</script>

<style scoped>
.field{position:relative;margin-bottom:18px}
.field input{
  width:100%;padding:18px 16px 8px;font-size:15px;color:var(--ink);
  background:rgba(255,255,255,.65);
  border:1px solid rgba(180,170,190,.35);border-radius:14px;outline:none;
  transition:border-color .25s var(--ease), box-shadow .25s var(--ease), background .25s var(--ease);
}
.field label{
  position:absolute;left:16px;top:15px;font-size:15px;color:var(--ink-3);pointer-events:none;
  transition:transform .2s var(--ease), color .2s var(--ease), font-size .2s var(--ease);
}
.field input:focus,.field input:not(:placeholder-shown){background:#fff;border-color:var(--accent);box-shadow:0 0 0 4px rgba(255,111,145,.16)}
.field input:focus+label,.field input:not(:placeholder-shown)+label{transform:translateY(-10px);font-size:11px;color:var(--ink-2)}
.field input:focus+label{color:var(--accent)}
</style>
