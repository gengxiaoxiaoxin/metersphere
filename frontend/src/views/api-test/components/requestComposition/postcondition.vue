<template>
  <condition
    ref="conditionRef"
    v-model:list="innerConfig.processors"
    :condition-types="conditionTypes"
    add-text="apiTestDebug.postCondition"
    :response="props.response"
    :disabled="props.disabled"
    :height-used="heightUsed"
    :sql-code-editor-height="props.sqlCodeEditorHeight"
    @change="emit('change')"
  >
    <template v-if="$slots.dropdownAppend" #dropdownAppend>
      <slot name="dropdownAppend" />
    </template>
    <template v-if="props.isDefinition" #titleRight>
      <a-switch
        v-model:model-value="innerConfig.enableGlobal"
        :disabled="props.disabled"
        size="small"
        type="line"
      ></a-switch>
      <div class="ml-[8px] text-[var(--color-text-1)]">{{ t('apiTestDebug.openGlobalPostCondition') }}</div>
      <a-tooltip :content="props.tipContent || t('apiTestDebug.openGlobalPostConditionTip')" position="left">
        <icon-question-circle
          class="ml-[4px] text-[var(--color-text-brand)] hover:text-[rgb(var(--primary-5))]"
          size="16"
        />
      </a-tooltip>
    </template>
  </condition>
</template>

<script setup lang="ts">
  import { useVModel } from '@vueuse/core';

  import condition from '@/views/api-test/components/condition/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  import { ExecuteConditionConfig, ExecuteConditionProcessor } from '@/models/apiTest/common';
  import { RequestConditionProcessor } from '@/enums/apiEnum';

  const props = defineProps<{
    config: ExecuteConditionConfig;
    secondBoxHeight?: number;
    layout: 'horizontal' | 'vertical';
    response?: string; // 响应内容
    isDefinition?: boolean; // 是否是定义页面
    isScenario?: boolean; // 是否是场景页面
    disabled?: boolean;
    sqlCodeEditorHeight?: string;
    tipContent?: string;
  }>();
  const emit = defineEmits<{
    (e: 'update:params', params: ExecuteConditionProcessor[]): void;
    (e: 'change'): void;
  }>();

  const { t } = useI18n();
  const innerConfig = useVModel(props, 'config', emit);
  const heightUsed = computed(() => {
    if (props.layout === 'horizontal') {
      return 328;
    }
    return 328 + (props.secondBoxHeight || 0);
  });

  const conditionTypes = computed(() => {
    if (props.isDefinition) {
      return [RequestConditionProcessor.SCRIPT, RequestConditionProcessor.SQL, RequestConditionProcessor.EXTRACT];
    }
    // 接口场景
    if (props.isScenario) {
      return [RequestConditionProcessor.SCRIPT, RequestConditionProcessor.SQL];
    }
    return [RequestConditionProcessor.SCRIPT];
  });

  const conditionRef = ref<InstanceType<typeof condition>>();
  watch(
    () => conditionRef.value?.activeItemId,
    (val) => {
      innerConfig.value.activeItemId = val;
    }
  );
</script>

<style lang="less" scoped></style>
