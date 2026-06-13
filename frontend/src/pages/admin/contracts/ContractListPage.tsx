import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, Drawer, message, Space } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { apiGet, apiPost } from '../../../app/request';

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

  useEffect(() => {
    if (!versionDrawerOpen || !activeContract) {
      return;
    }

    setLoadingVersions(true);
    apiGet<ContractVersionRow[]>(`/api/contracts/${activeContract.id}/versions`)
      .then(setVersions)
      .finally(() => setLoadingVersions(false));
  }, [activeContract, versionDrawerOpen]);

  const columns: ProColumns<ContractRow>[] = [
    { title: '订单 ID', dataIndex: 'orderId', ellipsis: true },
    { title: '合同编号', dataIndex: 'contractNo', hideInSearch: true },
    { title: '状态', dataIndex: 'status', hideInSearch: true },
    { title: '创建时间', dataIndex: 'createdAt', valueType: 'dateTime', hideInSearch: true },
    { title: '更新时间', dataIndex: 'updatedAt', valueType: 'dateTime', hideInSearch: true },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => (
        <Space size={8}>
          <Button
            type="link"
            size="small"
            onClick={() => {
              setActiveContract(record);
              setVersionDrawerOpen(true);
            }}
          >
            版本
          </Button>
          <Button
            type="link"
            size="small"
            onClick={async () => {
              await apiPost<ContractRow>(`/api/contracts/${record.id}/mark-offline-signed`);
              message.success('已标记线下签署');
              actionRef.current?.reload();
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
          setVersionDrawerOpen(false);
          setActiveContract(undefined);
          setVersions([]);
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
            { title: '金额', dataIndex: 'amount', valueType: 'money' },
            { title: '面积', dataIndex: 'areaSqm', valueType: 'digit' },
            { title: '签署状态', dataIndex: 'signStatus' },
            { title: '签署时间', dataIndex: 'signedAt', valueType: 'dateTime' },
            { title: '创建时间', dataIndex: 'createdAt', valueType: 'dateTime' },
          ]}
        />
      </Drawer>
    </PageContainer>
  );
}
