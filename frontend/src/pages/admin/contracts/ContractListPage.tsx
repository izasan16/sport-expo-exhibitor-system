import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, Drawer, message, Modal, Space } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { formatFriendlyTime, formatMoney, formatShortId } from '../../../app/formatters';
import { apiGet, apiPost } from '../../../app/request';
import { getContractStatusMeta, StatusTag } from '../../../app/status';

type ContractRow = {
  id: string;
  eventId: string;
  orderId: string;
  currentVersionId: string;
  contractNo: string;
  status: string;
  createdAt: string;
  updatedAt: string;
};

type ContractVersionRow = {
  id: string;
  eventId: string;
  contractId: string;
  versionNo: number;
  reason?: string;
  amount: number;
  areaSqm: number;
  signStatus: string;
  signedAt?: string;
  createdAt: string;
};

function buildQuery(params: Record<string, unknown>) {
  const orderId = params.orderId;
  return orderId ? `?orderId=${encodeURIComponent(String(orderId))}` : '';
}

export default function ContractListPage() {
  const actionRef = useRef<ActionType>();
  const [versionDrawerOpen, setVersionDrawerOpen] = useState(false);
  const [activeContract, setActiveContract] = useState<ContractRow>();
  const [versions, setVersions] = useState<ContractVersionRow[]>([]);
  const [loadingVersions, setLoadingVersions] = useState(false);
  const versionRequestRef = useRef(0);

  useEffect(() => {
    if (!versionDrawerOpen || !activeContract) {
      return;
    }

    const requestId = versionRequestRef.current + 1;
    versionRequestRef.current = requestId;
    let canceled = false;

    setVersions([]);
    setLoadingVersions(true);
    apiGet<ContractVersionRow[]>(`/api/contracts/${activeContract.id}/versions`)
      .then((data) => {
        if (!canceled && versionRequestRef.current === requestId) {
          setVersions(data);
        }
      })
      .finally(() => {
        if (!canceled && versionRequestRef.current === requestId) {
          setLoadingVersions(false);
        }
      });

    return () => {
      canceled = true;
    };
  }, [activeContract, versionDrawerOpen]);

  const columns: ProColumns<ContractRow>[] = [
    { title: '订单', dataIndex: 'orderId', ellipsis: true, render: (_, record) => formatShortId(record.orderId) },
    { title: '合同编号', dataIndex: 'contractNo', hideInSearch: true },
    {
      title: '状态',
      dataIndex: 'status',
      hideInSearch: true,
      render: (_, record) => <StatusTag meta={getContractStatusMeta(record.status)} />,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      hideInSearch: true,
      render: (_, record) => formatFriendlyTime(record.createdAt),
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      hideInSearch: true,
      render: (_, record) => formatFriendlyTime(record.updatedAt),
    },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => (
        <Space size={8}>
          <Button
            type="link"
            size="small"
            onClick={() => {
              setVersions([]);
              setActiveContract(record);
              setVersionDrawerOpen(true);
            }}
          >
            版本
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => {
              Modal.confirm({
                title: '标记线下签署？',
                content: `合同 ${record.contractNo || formatShortId(record.id)} 将标记为已线下签署。`,
                okText: '标记签署',
                cancelText: '取消',
                onOk: async () => {
                  await apiPost<ContractRow>(`/api/contracts/${record.id}/mark-offline-signed`);
                  message.success('已标记线下签署');
                  actionRef.current?.reload();
                },
              });
            }}
          >
            标记线下签署
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer title="合同管理">
      <ProTable<ContractRow>
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 'auto' }}
        columns={columns}
        locale={{ emptyText: '暂无合同记录' }}
        request={async (params) => {
          const data = await apiGet<ContractRow[]>(`/api/contracts${buildQuery(params)}`);
          return { data, success: true, total: data.length };
        }}
      />
      <Drawer
        title={activeContract ? `合同版本 - ${activeContract.contractNo}` : '合同版本'}
        width={720}
        open={versionDrawerOpen}
        destroyOnClose
        onClose={() => {
          versionRequestRef.current += 1;
          setVersionDrawerOpen(false);
          setActiveContract(undefined);
          setVersions([]);
          setLoadingVersions(false);
        }}
      >
        <ProTable<ContractVersionRow>
          rowKey="id"
          search={false}
          loading={loadingVersions}
          dataSource={versions}
          pagination={false}
          columns={[
            { title: '版本号', dataIndex: 'versionNo' },
            { title: '原因', dataIndex: 'reason' },
            { title: '金额', dataIndex: 'amount', render: (_, record) => formatMoney(record.amount) },
            { title: '面积', dataIndex: 'areaSqm', valueType: 'digit' },
            {
              title: '签署状态',
              dataIndex: 'signStatus',
              render: (_, record) => <StatusTag meta={getContractStatusMeta(record.signStatus)} />,
            },
            { title: '签署时间', dataIndex: 'signedAt', render: (_, record) => formatFriendlyTime(record.signedAt) },
            { title: '创建时间', dataIndex: 'createdAt', render: (_, record) => formatFriendlyTime(record.createdAt) },
          ]}
        />
      </Drawer>
    </PageContainer>
  );
}
