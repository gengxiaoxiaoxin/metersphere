<template>
  <MsDrawer
    v-model:visible="innerVisible"
    :title="t('ms.case.associate.title')"
    :width="1200"
    :footer="false"
    no-content-padding
    unmount-on-close
  >
    <template #headerLeft>
      <div class="float-left">
        <a-select
          v-if="props?.moduleOptions"
          v-model:model-value="caseType"
          class="ml-2 max-w-[100px]"
          :placeholder="t('caseManagement.featureCase.PleaseSelect')"
          @change="changeCaseTypeHandler"
        >
          <a-option v-for="item of props?.moduleOptions" :key="item.value" :value="item.value">
            {{ t(item.label) }}
          </a-option>
        </a-select>
      </div>
    </template>
    <div class="flex h-full">
      <div class="w-[292px] border-r border-[var(--color-text-n8)] p-[16px]">
        <div class="flex items-center justify-between">
          <div v-if="!props.hideProjectSelect" class="flex w-full flex-1">
            <a-select
              v-model="innerProject"
              class="mb-[16px] flex-1"
              :default-value="innerProject"
              allow-search
              :placeholder="t('common.pleaseSelect')"
            >
              <template #arrow-icon>
                <icon-caret-down />
              </template>
              <a-tooltip v-for="item of projectList" :key="item.id" :mouse-enter-delay="500" :content="item.name">
                <a-option :value="item.id" :class="item.id === innerProject ? 'arco-select-option-selected' : ''">
                  {{ item.name }}
                </a-option>
              </a-tooltip>
            </a-select>
          </div>
          <a-select v-if="caseType === 'API'" v-model="protocolType" class="mb-[16px] ml-2 max-w-[90px]">
            <a-option v-for="item of protocolOptions" :key="item" :value="item">{{ item }}</a-option>
          </a-select>
        </div>
        <a-input
          v-model:model-value="moduleKeyword"
          :placeholder="t('caseManagement.caseReview.folderSearchPlaceholder')"
          allow-clear
          class="mb-[16px]"
          :max-length="255"
        />
        <div class="folder">
          <div :class="getFolderClass('all')" @click="setActiveFolder('all')">
            <MsIcon type="icon-icon_folder_filled1" class="folder-icon" />
            <div class="folder-name">{{ t('caseManagement.featureCase.allCase') }}</div>
            <div class="folder-count">({{ modulesCount.total || modulesCount.all || 0 }})</div>
          </div>
          <div class="ml-auto flex items-center">
            <a-tooltip
              :content="isExpandAll ? t('project.fileManagement.collapseAll') : t('project.fileManagement.expandAll')"
            >
              <MsButton type="icon" status="secondary" class="!mr-0 p-[4px]" @click="expandHandler">
                <MsIcon :type="isExpandAll ? 'icon-icon_folder_collapse1' : 'icon-icon_folder_expansion1'" />
              </MsButton>
            </a-tooltip>
          </div>
        </div>
        <a-divider class="my-[8px]" />
        <a-spin class="w-full" :loading="moduleLoading">
          <MsTree
            v-model:selected-keys="selectedModuleKeys"
            :data="folderTree"
            :keyword="moduleKeyword"
            :empty-text="t('common.noData')"
            :virtual-list-props="virtualListProps"
            :field-names="{
              title: 'name',
              key: 'id',
              children: 'children',
              count: 'count',
            }"
            :expand-all="isExpandAll"
            block-node
            title-tooltip-position="left"
            @select="folderNodeSelect"
          >
            <template #title="nodeData">
              <div class="inline-flex w-full gap-[8px]">
                <div class="one-line-text w-full text-[var(--color-text-1)]">{{ nodeData.name }}</div>
                <div class="ms-tree-node-count ml-[4px] text-[var(--color-text-brand)]">{{ nodeData.count || 0 }}</div>
              </div>
            </template>
          </MsTree>
        </a-spin>
      </div>
      <div class="flex w-[calc(100%-293px)] flex-col p-[16px]">
        <MsAdvanceFilter
          v-model:keyword="keyword"
          :filter-config-list="filterConfigList"
          :custom-fields-config-list="searchCustomFields"
          :row-count="filterRowCount"
          :search-placeholder="t('caseManagement.featureCase.searchByNameAndId')"
          @keyword-search="searchCase"
          @adv-search="searchCase"
          @refresh="searchCase()"
        >
          <template #left>
            <div class="flex items-center justify-between">
              <div class="flex items-center">
                <div class="mr-[4px] text-[var(--color-text-1)]">{{ activeFolderName }}</div>
                <div class="text-[var(--color-text-4)]">({{ propsRes.msPagination?.total }})</div>
              </div>
            </div>
          </template>
        </MsAdvanceFilter>
        <ms-base-table v-bind="propsRes" no-disable class="mt-[16px]" v-on="propsEvent">
          <template #num="{ record }">
            <a-tooltip :content="`${record.num}`">
              <a-button type="text" class="px-0" @click="openDetail(record.id)">
                <div class="one-line-text max-w-[168px]">{{ record.num }}</div>
              </a-button>
            </a-tooltip>
          </template>
          <template #caseLevel="{ record }">
            <caseLevel v-if="getCaseLevel(record)" :case-level="getCaseLevel(record)" />
          </template>
        </ms-base-table>
        <div class="footer">
          <div class="flex flex-1 items-center">
            <slot name="footerLeft"></slot>
          </div>
          <div class="flex items-center">
            <slot name="footerRight">
              <a-button type="secondary" :disabled="props.confirmLoading" class="mr-[12px]" @click="cancel">
                {{ t('common.cancel') }}
              </a-button>
              <a-button
                type="primary"
                :loading="props.confirmLoading"
                :disabled="propsRes.selectedKeys.size === 0"
                @click="handleConfirm"
              >
                {{ t('ms.case.associate.associate') }}
              </a-button>
            </slot>
          </div>
        </div>
      </div>
    </div>
  </MsDrawer>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { useRouter } from 'vue-router';
  import { useVModel } from '@vueuse/core';

  import { CustomTypeMaps, MsAdvanceFilter } from '@/components/pure/ms-advance-filter';
  import { FilterFormItem, FilterType } from '@/components/pure/ms-advance-filter/type';
  import MsButton from '@/components/pure/ms-button/index.vue';
  import MsDrawer from '@/components/pure/ms-drawer/index.vue';
  import MsIcon from '@/components/pure/ms-icon-font/index.vue';
  import MsBaseTable from '@/components/pure/ms-table/base-table.vue';
  import { MsTableColumn } from '@/components/pure/ms-table/type';
  import useTable from '@/components/pure/ms-table/useTable';
  import MsProjectSelect from '@/components/business/ms-project-select/index.vue';
  import MsTree from '@/components/business/ms-tree/index.vue';
  import type { MsTreeNodeData } from '@/components/business/ms-tree/types';
  import caseLevel from './caseLevel.vue';

  import { getAssociatedProjectOptions, getCustomFieldsTable } from '@/api/modules/case-management/featureCase';
  import { useI18n } from '@/hooks/useI18n';
  import useAppStore from '@/store/modules/app';
  import { mapTree } from '@/utils';

  import type { CaseManagementTable } from '@/models/caseManagement/featureCase';
  import type { CommonList, ModuleTreeNode, TableQueryParams } from '@/models/common';
  import type { ProjectListItem } from '@/models/setting/project';
  import { CaseLinkEnum } from '@/enums/caseEnum';
  import { CaseManagementRouteEnum } from '@/enums/routeEnum';

  import type { CaseLevel } from './types';
  import { initGetModuleCountFunc, type RequestModuleEnum } from './utils';

  const router = useRouter();
  const appStore = useAppStore();
  const { t } = useI18n();

  const props = withDefaults(
    defineProps<{
      visible: boolean;
      projectId?: string; // 项目id
      caseId?: string; // 用例id  用例评审那边不需要传递
      getModulesFunc: (params: TableQueryParams) => Promise<ModuleTreeNode[]>; // 获取模块树请求
      modulesParams?: Record<string, any>; // 获取模块树请求
      getTableFunc: (params: TableQueryParams) => Promise<CommonList<CaseManagementTable>>; // 获取表请求函数
      tableParams?: TableQueryParams; // 查询表格的额外的参数
      okButtonDisabled?: boolean; // 确认按钮是否禁用
      currentSelectCase: keyof typeof CaseLinkEnum; // 当前选中的用例类型
      moduleOptions?: { label: string; value: string }[]; // 功能模块对应用例下拉
      confirmLoading: boolean;
      associatedIds: string[]; // 已关联用例id集合用于去重已关联
      hasNotAssociatedIds?: string[];
      type: RequestModuleEnum[keyof RequestModuleEnum];
      moduleCountParams?: TableQueryParams; // 获取模块树数量额外的参数
      hideProjectSelect?: boolean; // 是否隐藏项目选择
      isHiddenCaseLevel?: boolean;
    }>(),
    {
      isHiddenCaseLevel: false,
    }
  );

  const emit = defineEmits<{
    (e: 'update:visible', val: boolean): void;
    (e: 'update:projectId', val: string): void;
    (e: 'update:currentSelectCase', val: string | number | Record<string, any> | undefined): void;
    (e: 'init', val: TableQueryParams): void; // 初始化模块数量
    (e: 'close'): void;
    (e: 'save', params: any): void; // 保存对外传递关联table 相关参数
  }>();

  const virtualListProps = computed(() => {
    return {
      height: 'calc(100vh - 251px)',
      threshold: 200,
      fixedSize: true,
      buffer: 15, // 缓冲区默认 10 的时候，虚拟滚动的底部 padding 计算有问题
    };
  });

  const activeFolder = ref('all');
  const activeFolderName = ref(t('ms.case.associate.allCase'));
  const filterRowCount = ref(0);

  function getFolderClass(id: string) {
    return activeFolder.value === id ? 'folder-text folder-text--active' : 'folder-text';
  }

  const moduleKeyword = ref('');
  const folderTree = ref<ModuleTreeNode[]>([]);
  const moduleLoading = ref(false);

  const selectedModuleKeys = ref<string[]>([]);

  function setActiveFolder(id: string) {
    activeFolder.value = id;
    activeFolderName.value = t('ms.case.associate.allCase');
    selectedModuleKeys.value = [];
  }

  const innerVisible = useVModel(props, 'visible', emit);
  const innerProject = ref<string | undefined>(props.projectId);

  const protocolType = ref('HTTP'); // 协议类型
  const protocolOptions = ref(['HTTP']);
  const modulesCount = ref<Record<string, any>>({});
  const isExpandAll = ref(false);
  // 全部展开或折叠
  const expandHandler = () => {
    isExpandAll.value = !isExpandAll.value;
  };

  // 选中用例类型
  const caseType = computed({
    get() {
      return props.currentSelectCase;
    },
    set(val) {
      emit('update:currentSelectCase', val);
    },
  });

  /**
   * 初始化模块树
   * @param isSetDefaultKey 是否设置第一个节点为选中节点
   */
  async function initModules(isSetDefaultKey = false) {
    try {
      moduleLoading.value = true;
      let params = {
        projectId: innerProject.value,
        sourceType: props.moduleOptions && props.moduleOptions.length ? caseType.value : undefined,
        sourceId: props.moduleOptions && props.moduleOptions.length ? props.caseId : undefined,
      };
      if (props.modulesParams) {
        params = {
          ...params,
          ...props.modulesParams,
        };
      }
      const res = await props.getModulesFunc(params);
      folderTree.value = mapTree<ModuleTreeNode>(res, (e) => {
        return {
          ...e,
          hideMoreAction: e.id === 'root',
          draggable: false,
          disabled: false,
          count: modulesCount.value[e.id] || 0,
        };
      });
      if (isSetDefaultKey) {
        selectedModuleKeys.value = [folderTree.value[0].id];
        activeFolderName.value = folderTree.value[0].name;
        const offspringIds: string[] = [];
        mapTree(folderTree.value[0].children || [], (e) => {
          offspringIds.push(e.id);
          return e;
        });
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      moduleLoading.value = false;
    }
  }

  /**
   * 处理模块树节点选中事件
   */
  const offspringIds = ref<string[]>([]);

  function folderNodeSelect(_selectedKeys: (string | number)[], node: MsTreeNodeData) {
    selectedModuleKeys.value = _selectedKeys as string[];
    activeFolder.value = node.id;
    activeFolderName.value = node.name;
    offspringIds.value = [];
    mapTree(node.children || [], (e) => {
      offspringIds.value.push(e.id);
      return e;
    });
  }

  const keyword = ref('');
  const version = ref('');
  const projectList = ref<ProjectListItem[]>([]);

  function getCaseLevelColumn() {
    if (!props.isHiddenCaseLevel) {
      return [
        {
          title: 'ms.case.associate.caseLevel',
          dataIndex: 'caseLevel',
          slotName: 'caseLevel',
          width: 90,
        },
      ];
    }
    return [];
  }

  const columns: MsTableColumn = [
    {
      title: 'ID',
      dataIndex: 'num',
      slotName: 'num',
      sortIndex: 1,
      showTooltip: true,
      // sortable: {
      //   sortDirections: ['ascend', 'descend'],
      //   sorter: true,
      // },
      width: 150,
      fixed: 'left',
    },
    {
      title: 'ms.case.associate.caseName',
      dataIndex: 'name',
      // sortable: {
      //   sortDirections: ['ascend', 'descend'],
      //   sorter: true,
      // },
      showTooltip: true,
      width: 250,
    },
    ...getCaseLevelColumn(),
    {
      title: 'ms.case.associate.tags',
      dataIndex: 'tags',
      isTag: true,
    },
    {
      title: 'caseManagement.featureCase.tableColumnCreateUser',
      slotName: 'createUserName',
      dataIndex: 'createUserName',
      showTooltip: true,
      width: 200,
      showDrag: true,
    },
    {
      title: 'caseManagement.featureCase.tableColumnCreateTime',
      slotName: 'createTime',
      dataIndex: 'createTime',
      // sortable: {
      //   sortDirections: ['ascend', 'descend'],
      //   sorter: true,
      // },
      width: 200,
      showDrag: true,
    },
  ];

  watchEffect(() => {
    getCaseLevelColumn();
  });

  const { propsRes, propsEvent, loadList, setLoadListParams, resetSelector, setTableSelected } = useTable(
    props.getTableFunc,
    {
      columns,
      scroll: { x: '100%' },
      showSetting: false,
      selectable: true,
      showSelectAll: true,
      heightUsed: 310,
    },
    (record) => {
      return {
        ...record,
        tags: (record.tags || []).map((item: string, i: number) => {
          return {
            id: `${record.id}-${i}`,
            name: item,
          };
        }),
      };
    }
  );

  // 用例等级
  // TODO: 这个版本用例和接口以及场景不存在用例等级 不展示等级内容
  function getCaseLevel(record: CaseManagementTable) {
    if (record.customFields && record.customFields.length) {
      const caseItem = record.customFields.find((item: any) => item.fieldName === '用例等级' && item.internal);
      return caseItem?.options.find((item: any) => item.value === caseItem?.defaultValue).text;
    }
    return undefined;
  }

  const searchParams = ref<TableQueryParams>({
    moduleIds: [],
    version: version.value,
  });

  function getLoadListParams() {
    if (activeFolder.value === 'all' || !activeFolder.value) {
      searchParams.value.moduleIds = [];
    } else {
      searchParams.value.moduleIds = [activeFolder.value, ...offspringIds.value];
    }
    if (props.moduleOptions && props.moduleOptions.length) {
      searchParams.value.sourceType = caseType.value;
      searchParams.value.sourceId = props.caseId;
    }
    setLoadListParams({
      ...searchParams.value,
      ...props.tableParams,
      keyword: keyword.value,
      projectId: innerProject.value,
      excludeIds: [...props.associatedIds], // 已经存在的关联的id列表
      condition: {
        keyword: keyword.value,
      },
    });
    if (props.hasNotAssociatedIds && props.hasNotAssociatedIds.length > 0) {
      props.hasNotAssociatedIds.forEach((hasNotAssociatedId) => {
        setTableSelected(hasNotAssociatedId);
      });
    }
  }

  const combine = ref<Record<string, any>>({});
  const searchCustomFields = ref<FilterFormItem[]>([]);
  const filterConfigList = ref<FilterFormItem[]>([]);

  async function initFilter() {
    const result = await getCustomFieldsTable(appStore.currentProjectId);
    filterConfigList.value = [
      {
        title: 'caseManagement.featureCase.tableColumnID',
        dataIndex: 'id',
        type: FilterType.INPUT,
      },
      {
        title: 'caseManagement.featureCase.tableColumnName',
        dataIndex: 'name',
        type: FilterType.INPUT,
      },
      {
        title: 'caseManagement.featureCase.tableColumnModule',
        dataIndex: 'moduleId',
        type: FilterType.TREE_SELECT,
        treeSelectData: folderTree.value,
        treeSelectProps: {
          fieldNames: {
            title: 'name',
            key: 'id',
            children: 'children',
          },
        },
      },
      {
        title: 'caseManagement.featureCase.tableColumnVersion',
        dataIndex: 'versionId',
        type: FilterType.INPUT,
      },
      {
        title: 'caseManagement.featureCase.tableColumnCreateUser',
        dataIndex: 'createUser',
        type: FilterType.SELECT,
        selectProps: {
          mode: 'static',
          options: [],
        },
      },
      {
        title: 'caseManagement.featureCase.tableColumnCreateTime',
        dataIndex: 'createTime',
        type: FilterType.DATE_PICKER,
      },
      {
        title: 'bugManagement.createTime',
        dataIndex: 'createTime',
        type: FilterType.DATE_PICKER,
      },
      {
        title: 'caseManagement.featureCase.tableColumnUpdateUser',
        dataIndex: 'updateUser',
        type: FilterType.SELECT,
        selectProps: {
          mode: 'static',
          options: [],
        },
      },
      {
        title: 'caseManagement.featureCase.tableColumnUpdateTime',
        dataIndex: 'updateTime',
        type: FilterType.DATE_PICKER,
      },
      {
        title: 'caseManagement.featureCase.tableColumnTag',
        dataIndex: 'tags',
        type: FilterType.TAGS_INPUT,
      },
    ];
    // 处理系统自定义字段
    searchCustomFields.value = result.map((item: any) => {
      const FilterTypeKey: keyof typeof FilterType = CustomTypeMaps[item.type].type;
      const formType = FilterType[FilterTypeKey];
      const formObject = CustomTypeMaps[item.type];
      const { props: formProps } = formObject;
      const currentItem: any = {
        title: item.name,
        dataIndex: item.id,
        type: formType,
      };

      if (formObject.propsKey && formProps.options) {
        formProps.options = item.options;
        currentItem[formObject.propsKey] = {
          ...formProps,
        };
      }
      return currentItem;
    });
  }

  // 初始化模块数量
  async function initModuleCount() {
    try {
      const params = {
        keyword: keyword.value,
        moduleIds: selectedModuleKeys.value,
        projectId: innerProject.value,
        current: propsRes.value.msPagination?.current,
        pageSize: propsRes.value.msPagination?.pageSize,
        combine: combine.value,
        sourceId: props.caseId,
        sourceType: caseType.value,
        ...props.tableParams,
        ...props.moduleCountParams,
      };
      modulesCount.value = await initGetModuleCountFunc(props.type, params);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function initProjectList(setDefault: boolean) {
    try {
      projectList.value = await getAssociatedProjectOptions(appStore.currentOrgId, caseType.value);
      if (setDefault) {
        innerProject.value = projectList.value[0].id;
      }
    } catch (error) {
      console.log(error);
    }
  }

  function searchCase() {
    getLoadListParams();
    loadList();
    initModuleCount();
  }

  // 保存参数
  function handleConfirm() {
    const { excludeKeys, selectedKeys, selectorStatus } = propsRes.value;
    const { versionId, moduleIds } = searchParams.value;
    const params = {
      excludeIds: [...excludeKeys],
      selectIds: selectorStatus === 'all' ? [] : [...selectedKeys],
      selectAll: selectorStatus === 'all',
      moduleIds,
      versionId,
      refId: '',
      sourceType: caseType.value,
      projectId: innerProject.value,
      sourceId: props.caseId,
      totalCount: propsRes.value.msPagination?.total,
      condition: {
        keyword: keyword.value,
      },
    };

    emit('save', params);
  }

  function openDetail(id: string) {
    window.open(
      `${window.location.origin}#${
        router.resolve({ name: CaseManagementRouteEnum.CASE_MANAGEMENT_CASE }).fullPath
      }?id=${id}`
    );
  }

  function cancel() {
    innerVisible.value = false;
    keyword.value = '';
    version.value = '';
    searchParams.value = {
      moduleIds: [],
      version: '',
    };
    activeFolder.value = 'all';
    activeFolderName.value = t('ms.case.associate.allCase');
    resetSelector();
    emit('close');
  }

  watch(
    () => props.visible,
    (val) => {
      if (val) {
        if (!props.hideProjectSelect) {
          initProjectList(true);
        } else {
          resetSelector();
          initModules();
          searchCase();
          initFilter();
        }
      } else {
        cancel();
      }
    }
  );

  function changeCaseTypeHandler(
    value: string | number | boolean | Record<string, any> | (string | number | boolean | Record<string, any>)[]
  ) {
    caseType.value = value as keyof typeof CaseLinkEnum;
    initModules();
    searchCase();
  }

  watch(
    () => innerProject.value,
    (val) => {
      if (val) {
        resetSelector();
        initModules();
        searchCase();
        initFilter();
      }
    }
  );

  watch(
    () => activeFolder.value,
    () => {
      searchCase();
    }
  );

  /**
   * 初始化模块数量
   */
  watch(
    () => modulesCount.value,
    (obj) => {
      folderTree.value = mapTree<ModuleTreeNode>(folderTree.value, (node) => {
        return {
          ...node,
          count: obj?.[node.id] || 0,
        };
      });
    }
  );

  watch(
    () => props.currentSelectCase,
    () => {
      if (!props.hideProjectSelect) {
        initProjectList(true);
      }
      initModules();
      searchCase();
      initFilter();
    }
  );

  defineExpose({
    initModules,
  });
</script>

<style lang="less" scoped>
  .folder {
    @apply flex cursor-pointer items-center justify-between;

    padding: 8px 4px;
    border-radius: var(--border-radius-small);
    &:hover {
      background-color: rgb(var(--primary-1));
    }
    .folder-text {
      @apply flex cursor-pointer items-center;
      .folder-icon {
        margin-right: 4px;
        color: var(--color-text-4);
      }
      .folder-name {
        color: var(--color-text-1);
      }
      .folder-count {
        margin-left: 4px;
        color: var(--color-text-4);
      }
    }
    .folder-text--active {
      .folder-icon,
      .folder-name,
      .folder-count {
        color: rgb(var(--primary-5));
      }
    }
  }
  .footer {
    @apply flex items-center justify-between;

    margin: auto -16px -16px;
    padding: 12px 16px;
    box-shadow: 0 -1px 4px 0 rgb(31 35 41 / 10%);
  }
</style>
