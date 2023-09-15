// 核心的封装方法，详细参数看文档  https://arco.design/vue/component/table
// hook/table-props.ts

import { ref, watchEffect } from 'vue';
import dayjs from 'dayjs';
import { useAppStore, useTableStore } from '@/store';
import type { TableData } from '@arco-design/web-vue';
import type { TableQueryParams, CommonList } from '@/models/common';
import type { MsTableProps, MsTableDataItem, MsTableColumn, MsTableErrorStatus } from './type';

export interface Pagination {
  current: number;
  pageSize: number;
  total: number;
}

const appStore = useAppStore();
const tableStore = useTableStore();
export default function useTableProps<T>(
  loadListFunc: (v: TableQueryParams) => Promise<CommonList<MsTableDataItem<T>>>,
  props?: Partial<MsTableProps<T>>,
  // 数据处理的回调函数
  dataTransform?: (item: T) => TableData & T,
  // 编辑操作的保存回调函数
  saveCallBack?: (item: T) => Promise<any>
) {
  // 行选择
  const rowSelection = {
    type: 'checkbox',
    showCheckedAll: false,
  };

  const defaultProps: MsTableProps<T> = {
    tableKey: '',
    bordered: true,
    showPagination: true,
    size: 'default',
    heightUsed: 294,
    scroll: { x: 1400, y: appStore.innerHeight - 294 },
    checkable: true,
    loading: false,
    data: [],
    columns: [] as MsTableColumn,
    rowKey: 'id',
    selectedKeys: [],
    selectedAll: false,
    enableDrag: false,
    showSelectAll: true,
    showSetting: false,
    columnResizable: true,
    // 禁用 arco-table 的分页
    pagination: false,
    // 简易分页模式
    pageSimple: false,
    // 表格的错误状态
    tableErrorStatus: false,
    debug: false,
    // 展示第一行的操作
    showFirstOperation: false,
    ...props,
  };

  // 属性组
  const propsRes = ref<MsTableProps<T>>(defaultProps);
  const oldPagination = ref<Pagination>({
    current: 1,
    pageSize: appStore.pageSize,
    total: 0,
    showPageSize: appStore.showPageSize,
    showTotal: appStore.showTotal,
    showJumper: appStore.showJumper,
    hideOnSinglePage: appStore.hideOnSinglePage,
    simple: defaultProps.pageSimple,
  } as Pagination);

  // 排序
  const sortItem = ref<object>({});

  // 筛选
  const filterItem = ref<object>({});

  // keyword
  const keyword = ref('');

  // 是否分页
  if (propsRes.value.showPagination) {
    propsRes.value.msPagination = {
      current: 1,
      pageSize: appStore.pageSize,
      total: 0,
      showPageSize: appStore.showPageSize,
      showTotal: appStore.showTotal,
      showJumper: appStore.showJumper,
      hideOnSinglePage: appStore.hideOnSinglePage,
      simple: defaultProps.pageSimple,
    };
  }

  // 是否可选中
  if (propsRes.value.selectable) {
    propsRes.value.rowSelection = rowSelection;
  }

  // 是否可拖拽
  if (propsRes.value.enableDrag) {
    propsRes.value.draggable = { type: 'handle' };
  }

  // 加载效果
  const setLoading = (status: boolean) => {
    propsRes.value.loading = status;
  };

  // 设置表格错误状态
  const setTableErrorStatus = (status: MsTableErrorStatus) => {
    propsRes.value.tableErrorStatus = status;
  };

  // 如果表格设置了tableKey，设置缓存的分页大小
  if (propsRes.value.msPagination && typeof propsRes.value.msPagination === 'object' && propsRes.value.tableKey) {
    const pageSize = tableStore.getPageSize(propsRes.value.tableKey);
    propsRes.value.msPagination.pageSize = pageSize;
  }

  /**
   * 分页设置
   * @param current //当前页数
   * @param total //总页数默认是0条，可选
   * @param fetchData 获取列表数据,可选
   */
  interface SetPaginationPrams {
    current: number;
    total?: number;
  }

  const setPagination = ({ current, total }: SetPaginationPrams) => {
    if (propsRes.value.msPagination && typeof propsRes.value.msPagination === 'object') {
      propsRes.value.msPagination.current = current;
      if (total !== undefined) {
        propsRes.value.msPagination.total = total;
      }
    }
  };

  // 单独设置默认属性
  const setProps = (params: Partial<MsTableProps<T>>) => {
    const tmpProps = propsRes.value;
    Object.keys(params).forEach((key) => {
      tmpProps[key] = params[key];
    });
    propsRes.value = tmpProps;
  };

  // 设置请求参数，如果出了分页参数还有搜索参数，在模板页面调用此方法，可以加入参数
  const loadListParams = ref<object>({});
  const setLoadListParams = (params?: object) => {
    loadListParams.value = params || {};
  };

  const setKeyword = (v: string) => {
    keyword.value = v;
  };

  // 加载分页列表数据
  const loadList = async () => {
    const { current, pageSize } = propsRes.value.msPagination as Pagination;
    setLoading(true);
    try {
      const data = await loadListFunc({
        current,
        pageSize,
        sort: sortItem.value,
        filter: filterItem.value,
        keyword: keyword.value,
        ...loadListParams.value,
      });
      const tmpArr = data.list;
      (propsRes.value.data as MsTableDataItem<T>[]) = tmpArr.map((item) => {
        if (item.updateTime) {
          item.updateTime = dayjs(item.updateTime).format('YYYY-MM-DD HH:mm:ss');
        }
        if (item.createTime) {
          item.createTime = dayjs(item.createTime).format('YYYY-MM-DD HH:mm:ss');
        }
        if (dataTransform) {
          item = dataTransform(item);
        }
        return item;
      });
      if (data.total === 0) {
        setTableErrorStatus('empty');
      } else {
        setTableErrorStatus(false);
      }
      setPagination({ current: data.current, total: data.total });
      return data;
    } catch (err) {
      setTableErrorStatus('error');
    } finally {
      setLoading(false);
      // debug 模式下打印属性
      if (propsRes.value.debug && import.meta.env.DEV) {
        // eslint-disable-next-line no-console
        console.log('Table propsRes: ', propsRes.value);
      }
    }
  };

  // 重置页码和条数
  const resetPagination = () => {
    if (propsRes.value.msPagination) {
      propsRes.value.msPagination.current = 1;
      propsRes.value.msPagination.pageSize = appStore.pageSize;
    }
  };

  // 事件触发组
  const propsEvent = ref({
    // 排序触发
    sorterChange: (sortObj: { [key: string]: string }) => {
      sortItem.value = sortObj;
      loadList();
    },

    // 筛选触发
    filterChange: (dataIndex: string, filteredValues: string[]) => {
      filterItem.value = { [dataIndex]: filteredValues };
      loadList();
    },
    // 分页触发
    pageChange: async (current: number) => {
      setPagination({ current });
      await loadList();
    },
    // 修改每页显示条数
    pageSizeChange: (pageSize: number) => {
      if (propsRes.value.msPagination && typeof propsRes.value.msPagination === 'object') {
        propsRes.value.msPagination.pageSize = pageSize;
        if (propsRes.value.tableKey) {
          // 如果表格设置了tableKey，缓存分页大小
          tableStore.setPageSize(propsRes.value.tableKey, pageSize);
        }
      }
      loadList();
    },
    // 选择触发
    selectedChange: (arr: (string | number)[]) => {
      if (arr.length === 0) {
        propsRes.value.showPagination = true;
        propsRes.value.msPagination = oldPagination.value;
      } else {
        oldPagination.value = propsRes.value.msPagination as Pagination;
        propsRes.value.showPagination = false;
      }
      propsRes.value.selectedKeys = arr;
    },
    change: (_data: MsTableDataItem<T>[]) => {
      if (propsRes.value.draggable && _data instanceof Array) {
        (propsRes.value.data as MsTableDataItem<T>[]) = _data;
      }
    },
    // 编辑触发
    rowNameChange: (record: T) => {
      if (saveCallBack) {
        saveCallBack(record);
      }
    },
    // 重置排序
    resetSort: () => {
      sortItem.value = {};
    },
  });

  watchEffect(() => {
    // TODO 等UI出图，表格设置里加入分页配置等操作
    const { heightUsed } = propsRes.value;
    const currentY = appStore.innerHeight - (heightUsed || 294);
    propsRes.value.scroll = { ...propsRes.value.scroll, y: currentY };
  });

  return {
    propsRes,
    propsEvent,
    setProps,
    setLoading,
    loadList,
    setPagination,
    setLoadListParams,
    setKeyword,
    resetPagination,
  };
}
