<template>
  <div class="flex h-[calc(100%-64px)] flex-col" @click.stop="() => {}">
    <div v-if="isShowLoopControl" class="my-4 flex items-center justify-start" @click.stop="() => {}">
      <a-pagination
        v-model:page-size="controlPageSize"
        v-model:current="controlCurrent"
        :total="controlTotal"
        size="mini"
        show-total
        :show-jumper="controlTotal > 5"
        @change="loadControlLoop"
      />
      <!-- <loopPagination v-model:current-loop="controlCurrent" :loop-total="controlTotal" /> -->
    </div>
    <div class="mt-4 flex w-full items-center justify-between rounded bg-[var(--color-text-n9)] p-4">
      <div class="font-medium">
        <span
          :class="{ 'text-[rgb(var(--primary-5))]': activeType === 'ResContent' }"
          @click.stop="setActiveType('ResContent')"
          >{{ t('report.detail.api.resContent') }}</span
        >
        <span
          v-if="total > 0"
          :class="{ 'text-[rgb(var(--primary-5))]': activeType === 'SubRequest' }"
          @click.stop="setActiveType('SubRequest')"
        >
          <a-divider direction="vertical" :margin="8"></a-divider>
          {{ t('report.detail.api.subRequest') }}</span
        >
      </div>
      <div class="flex flex-row gap-6 text-center">
        <a-popover position="left" content-class="response-popover-content">
          <div
            v-if="activeStepDetailCopy?.content?.responseResult.responseCode"
            class="one-line-text max-w-[200px]"
            :style="{ color: statusCodeColor }"
          >
            {{ activeStepDetailCopy?.content?.responseResult.responseCode || '-' }}
          </div>
          <template #content>
            <div class="flex items-center gap-[8px] text-[14px]">
              <div class="text-[var(--color-text-4)]">{{ t('apiTestDebug.statusCode') }}</div>
              <div :style="{ color: statusCodeColor }">
                {{ activeStepDetailCopy?.content?.responseResult.responseCode || '-' }}
              </div>
            </div>
          </template>
        </a-popover>
        <a-popover position="left" content-class="w-[400px]">
          <div v-if="timingInfo?.responseTime" class="one-line-text text-[rgb(var(--success-7))]">
            {{ timingInfo?.responseTime || 0 }} ms
          </div>
          <template #content>
            <div class="mb-[8px] flex items-center gap-[8px] text-[14px]">
              <div class="text-[var(--color-text-4)]">{{ t('apiTestDebug.responseTime') }}</div>
              <div class="text-[rgb(var(--success-7))]"> {{ timingInfo?.responseTime }} ms </div>
            </div>
            <responseTimeLine v-if="timingInfo" :response-timing="timingInfo" />
          </template>
        </a-popover>
        <a-popover position="left" content-class="response-popover-content">
          <div
            v-if="activeStepDetail?.content?.responseResult.responseSize"
            class="one-line-text text-[rgb(var(--success-7))]"
          >
            {{ activeStepDetail?.content?.responseResult.responseSize || '-' }} bytes
          </div>
          <template #content>
            <div class="flex items-center gap-[8px] text-[14px]">
              <div class="text-[var(--color-text-4)]">{{ t('apiTestDebug.responseSize') }}</div>
              <div class="one-line-text text-[rgb(var(--success-7))]">
                {{ activeStepDetail?.content?.responseResult.responseSize }} bytes
              </div>
            </div>
          </template>
        </a-popover>
        <a-popover position="left" content-class="response-popover-content">
          <div v-if="props.showType && props.showType !== 'CASE'" class="one-line-text max-w-[150px]">{{
            props.environmentName
          }}</div>
          <template #content>
            <div v-if="props.showType && props.showType !== 'CASE'" class="one-line-text">{{
              props.environmentName
            }}</div>
          </template>
        </a-popover>
      </div>
    </div>
    <div v-if="activeType === 'SubRequest'" class="my-4 flex justify-start">
      <MsPagination
        v-model:page-size="pageSize"
        v-model:current="current"
        :total="total"
        size="mini"
        @change="loadLoop"
      />
    </div>
    <!-- 平铺 -->
    <a-spin v-if="props.mode === 'tiled'" class="w-full" :loading="loading">
      <Suspense>
        <TiledDisplay
          :menu-list="responseCompositionTabList"
          :request-result="activeStepDetailCopy?.content"
          :console="props.console"
          :is-definition="props.isDefinition"
          :report-id="props.reportId"
        />
      </Suspense>
    </a-spin>
    <!-- 响应内容tab -->
    <div v-else class="h-full">
      <a-spin :loading="loading" class="h-full w-full pb-1">
        <result
          v-model:active-tab="activeTab"
          :request-result="activeStepDetailCopy?.content"
          :console="props.console"
          :is-http-protocol="false"
          :request-url="activeStepDetail?.content.url"
          is-definition
          :is-priority-local-exec="false"
        />
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { useRoute } from 'vue-router';
  import { cloneDeep } from 'lodash-es';

  import result from '@/views/api-test/components/requestComposition/response/result.vue';

  import { reportCaseStepDetail, reportStepDetail } from '@/api/modules/api-test/report';
  import { useI18n } from '@/hooks/useI18n';

  import type { ReportStepDetail, ReportStepDetailItem, ScenarioItemType } from '@/models/apiTest/report';
  import { ResponseComposition, ScenarioStepType } from '@/enums/apiEnum';

  const TiledDisplay = defineAsyncComponent(() => import('./tiledDisplay.vue'));
  const responseTimeLine = defineAsyncComponent(() => import('@/views/api-test/components/responseTimeLine.vue'));
  const MsPagination = defineAsyncComponent(() => import('@/components/pure/ms-pagination/index'));

  const props = defineProps<{
    mode: 'tiled' | 'tab'; // 平铺 | tab形式
    stepItem?: ScenarioItemType; // 步骤详情
    console?: string;
    isPriorityLocalExec?: boolean;
    requestUrl?: string;
    isHttpProtocol?: boolean;
    isDefinition?: boolean;
    showType: 'API' | 'CASE';
    environmentName?: string; // 环境
    isResponseModel?: boolean;
    reportId?: string;
    steps?: ScenarioItemType[]; // 步骤列表
  }>();
  const { t } = useI18n();

  const responseCompositionTabList = [
    {
      label: t('apiTestDebug.responseBody'),
      value: ResponseComposition.BODY,
    },
    {
      label: t('apiTestDebug.responseHeader'),
      value: ResponseComposition.HEADER,
    },
    {
      label: t('apiTestDebug.realRequest'),
      value: ResponseComposition.REAL_REQUEST,
    },
    {
      label: t('apiTestDebug.console'),
      value: ResponseComposition.CONSOLE,
    },
    ...(props.isDefinition
      ? [
          {
            label: t('apiTestDebug.extract'),
            value: ResponseComposition.EXTRACT,
          },
          {
            label: t('apiTestDebug.assertion'),
            value: ResponseComposition.ASSERTION,
          },
        ]
      : []),
  ];

  const activeTab = ref(ResponseComposition.BODY);

  const activeStepDetail = ref<ReportStepDetailItem>();
  const activeStepDetailCopy = ref<ReportStepDetail>();
  const route = useRoute();

  const reportDetailMap = {
    API: {
      stepDetail: reportStepDetail,
    },
    CASE: {
      stepDetail: reportCaseStepDetail,
    },
  };
  const activeIndex = ref<number>(0);

  const total = computed(() => (activeStepDetail.value?.content?.subRequestResults || []).length);
  const current = ref(1);
  const pageSize = ref(1);

  const activeType = ref<'ResContent' | 'SubRequest'>('ResContent');
  const subRequestResults = ref<any[]>([]);

  /**
   *   加载子请求列表
   */
  async function loadLoop() {
    if (activeStepDetail.value?.content) {
      activeStepDetailCopy.value = {
        ...activeStepDetail.value,
        content: cloneDeep(subRequestResults.value[current.value - 1]),
      };
    }
  }

  const stepDetailInfo = ref<ReportStepDetailItem[]>([]);

  /**
   *   响应时间信息
   */
  const timingInfo = computed(() => {
    if (activeStepDetail.value && activeStepDetail.value.content.responseResult) {
      const {
        dnsLookupTime,
        downloadTime,
        latency,
        responseTime,
        socketInitTime,
        sslHandshakeTime,
        tcpHandshakeTime,
        transferStartTime,
      } = activeStepDetail.value.content.responseResult;
      return {
        dnsLookupTime,
        tcpHandshakeTime,
        sslHandshakeTime,
        socketInitTime,
        latency,
        downloadTime,
        transferStartTime,
        responseTime,
      };
    }
    return null;
  });

  /**
   *   响应状态码对应颜色
   */
  const statusCodeColor = computed(() => {
    if (activeStepDetailCopy.value?.content) {
      const code = Number(activeStepDetailCopy.value?.content?.responseResult.responseCode);
      if (code >= 200 && code < 300) {
        return 'rgb(var(--success-7)';
      }
      if (code >= 300 && code < 400) {
        return 'rgb(var(--warning-7)';
      }
      return 'rgb(var(--danger-7)';
    }
    return '';
  });

  const loading = ref<boolean>(false);
  const showApiType = ref<string[]>([
    ScenarioStepType.API,
    ScenarioStepType.API_CASE,
    ScenarioStepType.CUSTOM_REQUEST,
    ScenarioStepType.SCRIPT,
  ]);
  // 获取详情
  async function getStepDetail(stepId: string) {
    try {
      loading.value = true;
      if (props.stepItem) {
        const res = await reportDetailMap[props.showType].stepDetail(
          (props.stepItem?.reportId || props.reportId) as string,
          stepId,
          route.query.shareId as string | undefined
        );
        stepDetailInfo.value = cloneDeep(res) as any;
        // TODO 子请求后台数据不全--需要后边有数据进行测试
        activeStepDetail.value = stepDetailInfo.value[activeIndex.value];
        if (stepDetailInfo.value.length) {
          subRequestResults.value = stepDetailInfo.value[activeIndex.value].content.subRequestResults;
        }

        if (activeType.value === 'ResContent') {
          activeStepDetailCopy.value = cloneDeep(activeStepDetail.value);
        } else {
          loadLoop();
        }
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  /**
   * 设置激活类型是响应内容还是子请求
   */
  function setActiveType(type: 'ResContent' | 'SubRequest') {
    activeType.value = type;
    if (type === 'SubRequest') {
      loadLoop();
    } else {
      activeStepDetailCopy.value = { ...activeStepDetail.value };
    }
  }

  /**
   *  获取对应父是否为次数循环步骤
   */

  const isShowLoopControl = computed(() => {
    return (
      props.stepItem?.stepChildren &&
      props.stepItem?.stepChildren.length &&
      showApiType.value.includes(props.stepItem.stepType)
    );
  });

  const controlCurrent = ref<number>(1);
  const controlTotal = computed(() => {
    if (props.stepItem?.stepChildren) {
      return props.stepItem?.stepChildren.length || 0;
    }
    return 0;
  });
  const controlPageSize = ref(1);
  /**
   *  循环次数控制器
   */
  function loadControlLoop() {
    if (isShowLoopControl.value && props.stepItem?.stepChildren) {
      const loopStepId = props.stepItem?.stepChildren[controlCurrent.value - 1].stepId;
      if (loopStepId) {
        getStepDetail(loopStepId);
      }
    }
  }

  const originStepId = ref<string | undefined>('');

  watchEffect(() => {
    if (props.stepItem?.stepId && props.mode === 'tiled') {
      const stepIds = props.stepItem?.stepChildren || [];
      getStepDetail(isShowLoopControl.value ? stepIds[controlCurrent.value - 1].stepId : props.stepItem.stepId);
    }
  });

  onMounted(() => {
    originStepId.value = props.stepItem?.stepId;
    if (props.stepItem?.stepId && !props.stepItem.fold) {
      const stepIds = props.stepItem?.stepChildren || [];
      getStepDetail(isShowLoopControl.value ? stepIds[controlCurrent.value - 1].stepId : props.stepItem.stepId);
    }
  });
</script>

<style scoped lang="less"></style>
