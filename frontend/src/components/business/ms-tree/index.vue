<template>
  <div ref="treeContainerRef" :class="['ms-tree-container', containerStatusClass]">
    <a-tree
      v-show="filterTreeData.length > 0"
      v-bind="props"
      ref="treeRef"
      v-model:selected-keys="selectedKeys"
      v-model:checked-keys="checkedKeys"
      :data="filterTreeData"
      class="ms-tree"
      :allow-drop="handleAllowDrop"
      @drag-start="onDragStart"
      @drag-end="onDragEnd"
      @drop="onDrop"
      @select="select"
      @check="checked"
      @expand="expand"
    >
      <template #switcher-icon>
        <div class="hidden" @click.stop></div>
      </template>
      <template #title="_props">
        <div class="flex w-full items-center gap-[4px] overflow-hidden">
          <div
            v-if="_props.children && _props.children.length > 0"
            class="cursor-pointer"
            @click.stop="handleExpand(_props)"
          >
            <icon-caret-down v-if="_props.expanded" class="text-[var(--color-text-4)]" />
            <icon-caret-right v-else class="text-[var(--color-text-4)]" />
          </div>
          <div v-else class="h-full w-[16px]"></div>
          <a-tooltip
            v-if="$slots['title']"
            :content="_props[props.fieldNames.title]"
            :mouse-enter-delay="300"
            :position="props.titleTooltipPosition"
            :disabled="props.disabledTitleTooltip"
          >
            <span :class="props.titleClass || 'ms-tree-node-title'">
              <slot name="title" v-bind="_props"></slot>
            </span>
          </a-tooltip>
        </div>
      </template>
      <template v-if="$slots['drag-icon']" #drag-icon="_props">
        <slot name="title" v-bind="_props"></slot>
      </template>
      <template v-if="$slots['extra'] || props.nodeMoreActions" #extra="_props">
        <div class="sticky right-[8px] flex items-center justify-between">
          <div v-if="_props.hideMoreAction !== true" class="ms-tree-node-extra">
            <slot name="extra" v-bind="_props"></slot>
            <MsTableMoreAction
              v-if="
                props.nodeMoreActions &&
                (typeof props.filterMoreActionFunc === 'function'
                  ? props.filterMoreActionFunc(props.nodeMoreActions, _props)
                  : props.nodeMoreActions
                ).length > 0
              "
              :list="
                typeof props.filterMoreActionFunc === 'function'
                  ? props.filterMoreActionFunc(props.nodeMoreActions, _props)
                  : props.nodeMoreActions
              "
              trigger="click"
              @select="handleNodeMoreSelect($event, _props)"
              @close="moreActionsClose"
              @open="focusNodeKey = _props[props.fieldNames.key]"
            >
            </MsTableMoreAction>
          </div>
          <div class="ms-tree-node-extra-end">
            <slot name="extraEnd" v-bind="_props"></slot>
          </div>
        </div>
      </template>
    </a-tree>
    <slot name="empty">
      <div
        v-show="filterTreeData.length === 0 && props.emptyText"
        class="rounded-[var(--border-radius-small)] bg-[var(--color-fill-1)] p-[8px] text-[12px] leading-[16px] text-[var(--color-text-4)]"
      >
        {{ props.emptyText }}
      </div>
    </slot>
  </div>
</template>

<script setup lang="ts">
  import { nextTick, onBeforeMount, Ref, ref, watch } from 'vue';
  import { TreeInstance } from '@arco-design/web-vue';
  import { debounce } from 'lodash-es';

  import MsTableMoreAction from '@/components/pure/ms-table-more-action/index.vue';
  import type { ActionsItem } from '@/components/pure/ms-table-more-action/types';

  import useContainerShadow from '@/hooks/useContainerShadow';

  import type { MsTreeExpandedData, MsTreeFieldNames, MsTreeNodeData, MsTreeSelectedData } from './types';
  import { VirtualListProps } from '@arco-design/web-vue/es/_components/virtual-list-v2/interface';

  const props = withDefaults(
    defineProps<{
      keyword?: string; // 搜索关键字
      titleClass?: string; // 标题样式类
      searchDebounce?: number; // 搜索防抖 ms 数
      draggable?: boolean; // 是否可拖拽
      blockNode?: boolean; // 是否块级节点
      showLine?: boolean; // 是否展示连接线
      defaultExpandAll?: boolean; // 是否默认展开所有节点
      selectable?: boolean | ((node: MsTreeNodeData, info: { level: number; isLeaf: boolean }) => boolean); // 是否可选中
      fieldNames?: MsTreeFieldNames; // 自定义字段名
      nodeMoreActions?: ActionsItem[]; // 节点展示在省略号按钮内的更多操作
      nodeMoreActionSize?: 'medium' | 'mini' | 'small' | 'large'; // 更多操作按钮大小
      expandAll?: boolean; // 是否展开/折叠所有节点，true 为全部展开，false 为全部折叠
      emptyText?: string; // 空数据时的文案
      checkable?: boolean; // 是否可选中
      checkedStrategy?: 'all' | 'parent' | 'child'; // 选中节点时的策略
      virtualListProps?: VirtualListProps; // 虚拟滚动列表的属性
      disabledTitleTooltip?: boolean; // 是否禁用标题 tooltip
      actionOnNodeClick?: 'expand'; // 点击节点时的操作
      nodeHighlightClass?: string; // 节点高亮背景色
      titleTooltipPosition?:
        | 'top'
        | 'tl'
        | 'tr'
        | 'bottom'
        | 'bl'
        | 'br'
        | 'left'
        | 'lt'
        | 'lb'
        | 'right'
        | 'rt'
        | 'rb'; // 标题 tooltip 的位置
      allowDrop?: (dropNode: MsTreeNodeData, dropPosition: -1 | 0 | 1, dragNode?: MsTreeNodeData | null) => boolean; // 是否允许放置
      filterMoreActionFunc?: (items: ActionsItem[], node: MsTreeNodeData) => ActionsItem[]; // 过滤更多操作按钮
    }>(),
    {
      searchDebounce: 300,
      defaultExpandAll: false,
      selectable: true,
      draggable: false,
      titleTooltipPosition: 'top',
      fieldNames: () => ({
        key: 'key',
        title: 'title',
        children: 'children',
        isLeaf: 'isLeaf',
      }),
      disabledTitleTooltip: false,
    }
  );

  const emit = defineEmits<{
    (e: 'select', selectedKeys: Array<string | number>, node: MsTreeNodeData): void;
    (
      e: 'drop',
      tree: MsTreeNodeData[],
      dragNode: MsTreeNodeData, // 被拖拽的节点
      dropNode: MsTreeNodeData, // 放入的节点
      dropPosition: number // 放入的位置，-1 为放入节点前，1 为放入节点后，0 为放入节点内
    ): void;
    (e: 'moreActionSelect', item: ActionsItem, node: MsTreeNodeData): void;
    (e: 'moreActionsClose'): void;
    (e: 'check', val: Array<string | number>): void;
    (e: 'expand', node: MsTreeExpandedData): void;
  }>();

  const data = defineModel<MsTreeNodeData[]>('data', {
    required: true,
  });
  const selectedKeys = defineModel<(string | number)[]>('selectedKeys', {
    default: [],
  });
  const checkedKeys = defineModel<(string | number)[]>('checkedKeys', {
    default: [],
  });
  const focusNodeKey = defineModel<string | number>('focusNodeKey', {
    default: '',
  });

  const treeContainerRef: Ref = ref(null);
  const treeRef = ref<TreeInstance>();
  const { isInitListener, containerStatusClass, setContainer, initScrollListener } = useContainerShadow({
    overHeight: 32,
    containerClassName: 'ms-tree-container',
  });

  function init(isFirstInit = false) {
    nextTick(() => {
      if (isFirstInit) {
        if (props.defaultExpandAll) {
          treeRef.value?.expandAll(true);
        }
        if (!isInitListener.value && treeRef.value) {
          setContainer(
            props.virtualListProps?.height
              ? (treeRef.value.$el.querySelector('.arco-virtual-list') as HTMLElement)
              : treeRef.value.$el
          );
          initScrollListener();
        }
      }
    });
  }

  onBeforeMount(() => {
    init(true);
  });

  /**
   * 根据关键字过滤树节点
   * @param keyword 搜索关键字
   */
  function searchData(keyword: string) {
    const search = (_data: MsTreeNodeData[]) => {
      const result: MsTreeNodeData[] = [];
      _data.forEach((item) => {
        if (item[props.fieldNames.title].toLowerCase().includes(keyword.toLowerCase())) {
          result.push({ ...item, expanded: true });
        } else if (item[props.fieldNames.children]) {
          const filterData = search(item[props.fieldNames.children]);
          if (filterData.length) {
            result.push({
              ...item,
              expanded: true,
              [props.fieldNames.children]: filterData,
            });
          }
        }
      });
      return result;
    };

    return search(data.value);
  }

  const filterTreeData = ref<MsTreeNodeData[]>([]); // 初始化时全量的树数据或在非搜索情况下更新后的全量树数据

  // 防抖搜索
  const updateDebouncedSearch = debounce(() => {
    if (props.keyword) {
      filterTreeData.value = searchData(props.keyword);
      nextTick(() => {
        // 展开所有搜索到的节点(expandedKeys控制了节点展开，但是节点展开折叠图标未变化，需要手动触发展开事件)
        treeRef.value?.expandAll();
      });
    }
  }, props.searchDebounce);

  watch(
    () => data.value,
    (val) => {
      if (!props.keyword) {
        filterTreeData.value = val;
      } else {
        updateDebouncedSearch();
      }
    },
    {
      deep: true,
      immediate: true,
    }
  );

  watch(
    () => props.keyword,
    (val) => {
      if (!val) {
        filterTreeData.value = data.value;
      } else {
        updateDebouncedSearch();
      }
    }
  );

  function loop(
    _data: MsTreeNodeData[],
    key: string | number | undefined,
    callback: (item: MsTreeNodeData, index: number, arr: MsTreeNodeData[]) => void
  ) {
    _data.some((item, index, arr) => {
      if (item[props.fieldNames.key] === key) {
        callback(item, index, arr);
        return true;
      }
      if (item[props.fieldNames.children]) {
        return loop(item[props.fieldNames.children], key, callback);
      }
      return false;
    });
  }

  const tempDragNode = ref<MsTreeNodeData | null>(null);

  function handleAllowDrop({ dropNode, dropPosition }: { dropNode: MsTreeNodeData; dropPosition: -1 | 0 | 1 }) {
    if (props.allowDrop) {
      return props.allowDrop(dropNode, dropPosition, tempDragNode.value);
    }
    return true;
  }

  function onDragStart(e: DragEvent, node: MsTreeNodeData) {
    tempDragNode.value = node;
  }

  function onDragEnd() {
    tempDragNode.value = null;
  }

  /**
   * 处理拖拽结束
   */
  function onDrop({
    dragNode,
    dropNode,
    dropPosition,
  }: {
    dragNode: MsTreeNodeData; // 被拖拽的节点
    dropNode: MsTreeNodeData; // 放入的节点
    dropPosition: number; // 放入的位置，-1 为放入节点前，1 为放入节点后，0 为放入节点内
  }) {
    loop(data.value, dragNode.key, (item, index, arr) => {
      arr.splice(index, 1);
    });

    if (dropPosition === 0) {
      // 放入节点内
      loop(data.value, dropNode.key, (item) => {
        item.children = item.children || [];
        item.children.push(dragNode);
      });
      dropNode.isLeaf = false;
      if (props.showLine) {
        delete dropNode.switcherIcon; // 放入的节点的 children 放入了被拖拽的节点，需要删除 switcherIcon 以展示默认的折叠图标
      }
    } else {
      // 放入节点前或后
      loop(data.value, dropNode.key, (item, index, arr) => {
        arr.splice(dropPosition < 0 ? index : index + 1, 0, dragNode);
      });
    }
    emit('drop', data.value, dragNode, dropNode, dropPosition);
  }

  /**
   * 处理树节点选中（非复选框）
   */
  function select(_selectedKeys: Array<string | number>, _data: MsTreeSelectedData) {
    emit('select', _selectedKeys, _data.selectedNodes[0]);
  }

  function checked(_checkedKeys: Array<string | number>) {
    emit('check', _checkedKeys);
  }

  const focusEl = ref<HTMLElement | null>(); // 存储聚焦的节点元素

  watch(
    () => focusNodeKey.value,
    (val) => {
      if (val?.toString() !== '') {
        focusEl.value = treeRef.value?.$el.querySelector(`[data-key="${val}"]`);
        if (focusEl.value) {
          focusEl.value.classList.add(props.nodeHighlightClass || 'ms-tree-node-focus');
        }
      } else if (focusEl.value) {
        focusEl.value.classList.remove(props.nodeHighlightClass || 'ms-tree-node-focus');
      }
    }
  );

  /**
   * 处理树节点更多按钮事件
   * @param item
   */
  function handleNodeMoreSelect(item: ActionsItem, node: MsTreeNodeData) {
    emit('moreActionSelect', item, node);
  }

  function moreActionsClose() {
    emit('moreActionsClose');
  }

  watch(
    () => props.expandAll,
    (val) => {
      if (typeof val === 'boolean') {
        treeRef.value?.expandAll(val);
      }
    }
  );

  function handleExpand(node: MsTreeNodeData) {
    node.expanded = !node.expanded;
    treeRef.value?.expandNode(node[props.fieldNames.key], node.expanded);
  }

  function expand(expandKeys: Array<string | number>, node: MsTreeExpandedData) {
    emit('expand', node);
  }

  function checkAll(val: boolean) {
    treeRef.value?.checkAll(val);
  }

  function expandNode(key: (string | number)[] | (string | number), expanded: boolean) {
    treeRef.value?.expandNode(key, expanded);
  }

  defineExpose({
    checkAll,
    expandNode,
  });
</script>

<style lang="less">
  .ms-tree-container {
    .ms-container--shadow-y();
    @apply h-full;
    .ms-tree {
      .ms-scroll-bar();
      @apply h-full overflow-auto;
      .arco-tree-node {
        border-radius: var(--border-radius-small);
        &:hover {
          background-color: rgb(var(--primary-1));
          .arco-tree-node-title {
            background-color: rgb(var(--primary-1));
            &:not([draggable='false']) {
              .arco-tree-node-title-text {
                width: 100%;
              }
            }
          }
          .ms-tree-node-count {
            @apply hidden;
          }
          .ms-tree-node-extra {
            @apply visible w-auto;
          }
        }
        .arco-tree-node-indent-block {
          width: 1px;
        }
        .arco-tree-node-minus-icon,
        .arco-tree-node-plus-icon {
          border: 1px solid var(--color-text-4);
          border-radius: var(--border-radius-mini);
          background-color: white;
          &::after,
          &::before {
            background-color: var(--color-text-4);
          }
        }
        .arco-tree-node-switcher {
          @apply hidden;
          // .arco-tree-node-switcher-icon {
          //   @apply flex;

          //   color: var(--color-text-4);
          // }
        }
        .arco-tree-node-title-highlight {
          background-color: transparent;
        }
        .arco-tree-node-title {
          &:hover {
            background-color: rgb(var(--primary-1));
            + .ms-tree-node-extra {
              @apply visible w-auto;
            }
          }
          .arco-tree-node-title-text {
            width: 100%;
            .ms-tree-node-title {
              @apply flex-1 overflow-hidden;
            }
          }
        }
        .arco-tree-node-title-block {
          width: 60%;
        }
        .ms-tree-node-extra {
          @apply invisible flex w-0 items-center;

          margin-left: -4px;
          height: 32px;
          border-radius: var(--border-radius-small);
          background-color: rgb(var(--primary-1));
          &:hover {
            @apply visible w-auto;
          }
          .ms-tree-node-extra__btn,
          .ms-tree-node-extra__more {
            padding: 4px;
            border-radius: var(--border-radius-mini);
            &:hover {
              background-color: rgb(var(--primary-9));
              .arco-icon {
                color: rgb(var(--primary-5));
              }
            }
          }
          .ms-tree-node-extra__more {
            margin-right: 4px;
          }
        }
        .ms-tree-node-extra-end {
          @apply flex items-center;
        }
        .arco-tree-node-custom-icon {
          @apply hidden;
        }
      }
      .ms-tree-node-focus {
        background-color: rgb(var(--primary-1));
        .arco-tree-node-title {
          background-color: rgb(var(--primary-1));
        }
        .ms-tree-node-extra {
          @apply visible w-auto;
        }
      }
      .arco-tree-node-selected {
        background-color: rgb(var(--primary-1));
        .arco-tree-node-minus-icon,
        .arco-tree-node-plus-icon {
          border: 1px solid rgb(var(--primary-5));
          border-radius: var(--border-radius-mini);
          background-color: white;
          &::after,
          &::before {
            background-color: rgb(var(--primary-5));
          }
        }
        .arco-tree-node-switcher-icon .arco-icon,
        .arco-tree-node-title {
          font-weight: 500 !important;
          color: rgb(var(--primary-5));
          .ms-tree-node-title {
            color: rgb(var(--primary-5));
            * {
              color: rgb(var(--primary-5));
            }
          }
        }
        .arco-tree-node-title {
          background-color: rgb(var(--primary-1));
        }
      }
      .arco-tree-node-disabled {
        &:hover {
          background-color: transparent;
        }
        * {
          color: var(--color-text-4) !important;
        }
        .arco-tree-node-title {
          &:hover {
            background-color: transparent;
          }
        }
      }
      .arco-tree-node-disabled-selectable {
        @apply cursor-default;
        .arco-tree-node-title {
          @apply cursor-default;
        }
      }
    }
  }
  // 为了让挂载在 body 下非 app 内的树节点生效
  .arco-tree-node-drag-icon {
    @apply hidden;
  }
</style>
