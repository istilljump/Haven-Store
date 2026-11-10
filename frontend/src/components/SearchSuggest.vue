<template>
  <el-autocomplete
    :model-value="modelValue"
    :fetch-suggestions="querySearch"
    :placeholder="placeholder"
    :trigger-on-focus="false"
    :debounce="200"
    value-key="word"
    size="large"
    clearable
    class="search-suggest"
    @update:model-value="(v) => emit('update:modelValue', v)"
    @select="handleSelect"
    @keyup.enter="handleEnter"
  >
    <template #default="{ item }">
      <div class="suggest-item">
        <span class="suggest-word">{{ item.word }}</span>
        <span v-if="item.tagLabel" class="suggest-tag">【{{ item.tagLabel }}】</span>
        <span v-if="item.dealHint" class="suggest-hint">【{{ item.dealHint }}】</span>
        <span v-if="item.source === 'CORRECTION'" class="suggest-correct">猜你想搜</span>
      </div>
    </template>
    <template #append>
      <el-button type="primary" @click="handleEnter">搜索</el-button>
    </template>
  </el-autocomplete>
</template>

<script setup>
import searchApi from '@/api/search'

/**
 * 增强搜索框（校园闲置增强搜索的前端入口）
 * <p>
 * 基于 el-autocomplete 封装：词库/拼音/本校热搜三级候选，
 * 每条附带小字标签【教材】【本校近期成交12件】；纠错建议以「猜你想搜」呈现。
 * 选择候选或回车即向上抛出 search 事件，由页面决定跳转方式
 */
const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '搜索校园闲置：教材、自行车、显示器…' },
  campusId: { type: Number, default: null }
})

const emit = defineEmits(['update:modelValue', 'search'])

/**
 * el-autocomplete 的候选拉取：走 /search/suggest（200ms 防抖由组件处理）
 */
const querySearch = async (queryString, callback) => {
  try {
    const data = await searchApi.getSuggest({
      q: queryString || undefined,
      campusId: props.campusId || undefined,
      limit: 8
    })
    callback((data && data.items) || [])
  } catch (err) {
    // 补全失败静默降级为无候选，不影响手动输入搜索
    callback([])
  }
}

const handleSelect = (item) => {
  if (item && item.word) {
    emit('update:modelValue', item.word)
    emit('search', item.word)
  }
}

const handleEnter = () => {
  emit('search', props.modelValue)
}
</script>

<style scoped>
.search-suggest {
  width: 100%;
}

.suggest-item {
  display: flex;
  align-items: center;
  gap: 4px;
  line-height: 1.4;
}

.suggest-word {
  color: #303133;
}

.suggest-tag {
  font-size: 12px;
  color: #909399;
}

.suggest-hint {
  font-size: 12px;
  color: #e6a23c;
}

.suggest-correct {
  font-size: 12px;
  color: #67c23a;
}
</style>
