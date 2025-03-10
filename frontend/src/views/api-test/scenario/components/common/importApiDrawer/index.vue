<template>
  <MsDrawer
    v-model:visible="visible"
    :title="props.singleSelect ? t('common.replace') : t('apiScenario.importSystemApi')"
    :width="1200"
    no-content-padding
    disabled-width-drag
  >
    <div class="h-full w-full overflow-hidden">
      <a-tabs v-model:active-key="activeKey" @change="resetModule">
        <a-tab-pane key="api" :title="t('apiScenario.api')" />
        <a-tab-pane key="case" :title="t('apiScenario.case')" />
        <a-tab-pane key="scenario" :title="t('apiScenario.scenario')" />
      </a-tabs>
      <a-divider :margin="0"></a-divider>
      <div class="flex h-[calc(100%-49px)]">
        <div class="w-[300px] border-r p-[16px]">
          <div class="flex flex-col">
            <div class="mb-[12px] flex items-center gap-[8px]">
              <MsProjectSelect v-model:project="currentProject" @change="resetModule" />
              <a-select
                v-if="activeKey !== 'scenario'"
                v-model:model-value="protocol"
                :options="protocolOptions"
                class="w-[90px]"
                @change="resetModule"
              />
            </div>
            <moduleTree
              ref="moduleTreeRef"
              :type="activeKey"
              :project-id="currentProject"
              :protocol="protocol"
              @select="handleModuleSelect"
            />
          </div>
        </div>
        <div class="table-container">
          <apiTable
            ref="apiTableRef"
            :module="activeModule"
            :type="activeKey"
            :protocol="protocol"
            :project-id="currentProject"
            :module-ids="moduleIds"
            :selected-apis="selectedApis"
            :selected-cases="selectedCases"
            :selected-scenarios="selectedScenarios"
            :scenario-id="props.scenarioId"
            :case-id="props.caseId"
            :api-id="props.apiId"
            :single-select="props.singleSelect"
            :search-placeholder="
              activeKey === 'scenario' ? t('apiScenario.quoteTableSearchTip2') : t('apiScenario.quoteTableSearchTip')
            "
            @select="handleTableSelect"
          />
        </div>
      </div>
    </div>
    <template #footer>
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-[4px]">
          <div class="second-text">{{ t('apiScenario.sumSelected') }}</div>
          <div class="main-text">{{ totalSelected }}</div>
          <a-divider direction="vertical" :margin="4"></a-divider>
          <div class="second-text">{{ t('apiScenario.api') }}</div>
          <div class="main-text">{{ selectedApis.length }}</div>
          <a-divider direction="vertical" :margin="4"></a-divider>
          <div class="second-text">{{ t('apiScenario.case') }}</div>
          <div class="main-text">{{ selectedCases.length }}</div>
          <a-divider direction="vertical" :margin="4"></a-divider>
          <div class="second-text">{{ t('apiScenario.scenario') }}</div>
          <div class="main-text">{{ selectedScenarios.length }}</div>
          <a-divider v-show="totalSelected > 0" direction="vertical" :margin="4"></a-divider>
          <MsButton
            v-show="totalSelected > 0 && !props.singleSelect"
            type="text"
            class="!mr-0 ml-[4px]"
            @click="clearAll"
          >
            {{ t('common.clear') }}
          </MsButton>
        </div>
        <div class="flex items-center gap-[12px]">
          <a-button type="secondary" :disabled="loading" @click="handleCancel">{{ t('common.cancel') }}</a-button>
          <a-button type="primary" :loading="loading" :disabled="totalSelected === 0" @click="handleCopy">
            {{ t('common.copy') }}
          </a-button>
          <a-button type="primary" :loading="loading" :disabled="totalSelected === 0" @click="handleQuote">
            {{ t('common.quote') }}
          </a-button>
        </div>
      </div>
    </template>
  </MsDrawer>
</template>

<script setup lang="ts">
  import { SelectOptionData } from '@arco-design/web-vue';
  import { cloneDeep } from 'lodash-es';

  import MsButton from '@/components/pure/ms-button/index.vue';
  import MsDrawer from '@/components/pure/ms-drawer/index.vue';
  import { MsTableDataItem } from '@/components/pure/ms-table/type';
  import { MsTreeNodeData } from '@/components/business/ms-tree/types';
  import moduleTree from './moduleTree.vue';
  import MsProjectSelect from './projectSelect.vue';
  import apiTable from './table.vue';

  import { getProtocolList } from '@/api/modules/api-test/common';
  import { getSystemRequest } from '@/api/modules/api-test/scenario';
  import { useI18n } from '@/hooks/useI18n';
  import useAppStore from '@/store/modules/app';
  import { getGenerateId, mapTree } from '@/utils';

  import type { ApiCaseDetail, ApiDefinitionDetail } from '@/models/apiTest/management';
  import type { ApiScenarioTableItem } from '@/models/apiTest/scenario';
  import { ScenarioStepRefType, ScenarioStepType } from '@/enums/apiEnum';

  export interface ImportData {
    api: MsTableDataItem<ApiDefinitionDetail>[];
    case: MsTableDataItem<ApiCaseDetail>[];
    scenario: MsTableDataItem<ApiScenarioTableItem>[];
  }

  const props = defineProps<{
    scenarioId?: string | number;
    caseId?: string | number;
    apiId?: string | number;
    singleSelect?: boolean;
  }>();
  const emit = defineEmits<{
    (e: 'copy', data: ImportData): void;
    (e: 'quote', data: ImportData): void;
  }>();

  const appStore = useAppStore();
  const { t } = useI18n();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });
  const activeKey = ref<'api' | 'case' | 'scenario'>('api');

  const loading = ref(false);
  const selectedApis = ref<MsTableDataItem<ApiDefinitionDetail>[]>([]);
  const selectedCases = ref<MsTableDataItem<ApiCaseDetail>[]>([]);
  const selectedScenarios = ref<MsTableDataItem<ApiScenarioTableItem>[]>([]);
  const totalSelected = computed(() => {
    return selectedApis.value.length + selectedCases.value.length + selectedScenarios.value.length;
  });

  function handleTableSelect(data: MsTableDataItem<ApiCaseDetail | ApiDefinitionDetail | ApiScenarioTableItem>[]) {
    if (props.singleSelect) {
      selectedApis.value = [];
      selectedCases.value = [];
      selectedScenarios.value = [];
    }
    if (activeKey.value === 'api') {
      selectedApis.value = data as MsTableDataItem<ApiDefinitionDetail>[];
    } else if (activeKey.value === 'case') {
      selectedCases.value = data as MsTableDataItem<ApiCaseDetail>[];
    } else if (activeKey.value === 'scenario') {
      selectedScenarios.value = data as MsTableDataItem<ApiScenarioTableItem>[];
    }
  }

  const activeModule = ref<MsTreeNodeData>({});
  const currentProject = ref(appStore.currentProjectId);
  const protocol = ref('HTTP');
  const protocolOptions = ref<SelectOptionData[]>([]);
  const protocolLoading = ref(false);

  async function initProtocolList() {
    try {
      protocolLoading.value = true;
      const res = await getProtocolList(appStore.currentOrgId);
      protocolOptions.value = res.map((e) => ({
        label: e.protocol,
        value: e.protocol,
        polymorphicName: e.polymorphicName,
        pluginId: e.pluginId,
      }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      protocolLoading.value = false;
    }
  }

  const moduleTreeRef = ref<InstanceType<typeof moduleTree>>();
  const apiTableRef = ref<InstanceType<typeof apiTable>>();
  const moduleIds = ref<(string | number)[]>([]);

  function resetModule() {
    nextTick(() => {
      moduleTreeRef.value?.init(activeKey.value);
    });
  }

  function handleModuleSelect(ids: (string | number)[], node: MsTreeNodeData) {
    activeModule.value = node;
    moduleIds.value = ids;
    apiTableRef.value?.loadPage(ids);
  }

  function clearAll() {
    selectedApis.value = [];
    selectedCases.value = [];
    selectedScenarios.value = [];
  }

  function handleCancel() {
    clearAll();
    visible.value = false;
  }

  /**
   * 获取复制或引用的步骤数据
   * @param refType 复制或引用
   */
  async function getScenarioSteps(
    refType: ScenarioStepRefType.COPY | ScenarioStepRefType.REF,
    other: {
      api: MsTableDataItem<ApiDefinitionDetail>[];
      case: MsTableDataItem<ApiCaseDetail>[];
    }
  ) {
    const scenarioMap: Record<string, MsTableDataItem<ApiScenarioTableItem>[]> = {};
    // 可以跨项目选择，但是接口的项目 id 是单个，所以需要按项目分组
    selectedScenarios.value.forEach((e) => {
      if (!scenarioMap[e.projectId]) {
        scenarioMap[e.projectId] = [];
      }
      scenarioMap[e.projectId].push(e);
    });
    const scenarioRequestArr: any[] = [];
    Object.keys(scenarioMap).forEach((projectId) => {
      // 组装请求
      scenarioRequestArr.push(
        getSystemRequest({
          scenarioRequest: {
            projectId,
            unselectedIds: [],
            selectedIds: scenarioMap[projectId].map((e) => e.id),
          },
          refType,
        })
      );
    });
    try {
      loading.value = true;
      const allRes = await Promise.all(scenarioRequestArr);
      let fullScenarioArr: MsTableDataItem<ApiScenarioTableItem>[] = [];
      allRes.forEach((res) => {
        fullScenarioArr.push(...res);
      });
      if (refType === ScenarioStepRefType.COPY) {
        // 复制需要递归给每个节点生成新的uniqueId，并记录copyFromStepId
        fullScenarioArr = mapTree<MsTableDataItem<ApiScenarioTableItem>>(fullScenarioArr, (node) => {
          const id = getGenerateId();
          if (
            node.parent &&
            node.parent.stepType === ScenarioStepType.API_SCENARIO &&
            [ScenarioStepRefType.REF, ScenarioStepRefType.PARTIAL_REF].includes(node.parent.refType)
          ) {
            // 如果根节点是引用场景
            node.isQuoteScenarioStep = true; // 标记为引用场景下的子步骤
            node.isRefScenarioStep = node.parent.refType === ScenarioStepRefType.REF; // 标记为完全引用场景
            node.draggable = false; // 引用场景下的任何步骤不可拖拽
          } else if (node.parent) {
            // 如果有父节点
            node.isQuoteScenarioStep = node.parent.isQuoteScenarioStep; // 复用父节点的引用场景标记
            node.isRefScenarioStep = node.parent.isRefScenarioStep; // 复用父节点的是否完全引用场景标记
          }
          return {
            ...node,
            copyFromStepId: node.id,
            originProjectId: node.projectId,
            id: node.isQuoteScenarioStep ? node.id : id, // 引用场景下的步骤 id 不变，其他情况的步骤生成新的 id
            uniqueId: id,
          };
        });
        emit(
          'copy',
          cloneDeep({
            api: other.api,
            case: other.case,
            scenario: fullScenarioArr,
          })
        );
        handleCancel();
      } else {
        // 引用只需要给场景节点生成新的步骤 id，内部步骤只需要生成uniqueId作为前端渲染 id
        fullScenarioArr = fullScenarioArr.map((e) => {
          const id = getGenerateId();
          return {
            ...e,
            children: mapTree<MsTableDataItem<ApiScenarioTableItem>>(e.children || [], (node) => {
              const childId = getGenerateId();
              return {
                ...node,
                originProjectId: node.projectId,
                uniqueId: childId,
                isQuoteScenarioStep: true,
                isRefScenarioStep: true, // 默认是完全引用的
                draggable: false,
              };
            }),
            id,
            uniqueId: id,
            originProjectId: e.projectId,
            draggable: false,
          };
        });
        emit(
          'quote',
          cloneDeep({
            api: other.api,
            case: other.case,
            scenario: fullScenarioArr,
          })
        );
        handleCancel();
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function handleCopy() {
    const copyApis = selectedApis.value.map((e) => ({
      ...e,
      originProjectId: e.projectId,
    }));
    const copyCases = selectedCases.value.map((e) => ({
      ...e,
      originProjectId: e.projectId,
    }));
    if (selectedScenarios.value.length > 0) {
      await getScenarioSteps(ScenarioStepRefType.COPY, {
        api: copyApis,
        case: copyCases,
      });
    } else {
      emit(
        'copy',
        cloneDeep({
          api: copyApis,
          case: copyCases,
          scenario: selectedScenarios.value,
        })
      );
      handleCancel();
    }
  }

  async function handleQuote() {
    const quoteApis = selectedApis.value.map((e) => ({
      ...e,
      originProjectId: e.projectId,
    }));
    const quoteCases = selectedCases.value.map((e) => ({
      ...e,
      originProjectId: e.projectId,
    }));
    if (selectedScenarios.value.length > 0) {
      await getScenarioSteps(ScenarioStepRefType.REF, {
        api: quoteApis,
        case: quoteCases,
      });
    } else {
      emit(
        'quote',
        cloneDeep({
          api: quoteApis,
          case: quoteCases,
          scenario: selectedScenarios.value,
        })
      );
      handleCancel();
    }
  }

  onBeforeMount(() => {
    initProtocolList();
  });

  // 外面需要使用 v-if 动态渲染
  onMounted(() => {
    nextTick(() => {
      // 外面使用 v-if 动态渲染时，需要在 nextTick 中执行初始化数据，因为子组件 ref 引用需要在渲染后才能获取到
      moduleTreeRef.value?.init(activeKey.value);
    });
  });
</script>

<style lang="less" scoped>
  .second-text {
    color: var(--color-text-2);
  }
  .main-text {
    color: rgb(var(--primary-5));
  }
  :deep(.arco-tabs-content) {
    @apply hidden;
  }
  .table-container {
    @apply overflow-auto;
    .ms-scroll-bar();

    padding: 16px;
    width: calc(100% - 300px);
  }
</style>
