import { PageContainer, ProCard, ProDescriptions, ProForm, ProFormText, ProFormTextArea } from '@ant-design/pro-components';
import type { ProDescriptionsItemProps } from '@ant-design/pro-components';
import { Button, message, Result, Skeleton, Space, Tag } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { formatExhibitorStatus } from '../../../app/display';
import type { Exhibitor } from '../../../app/domain';
import { apiGet, apiPut } from '../../../app/request';

type ProfileValues = {
  companyName: string;
  creditCode?: string;
  contactName?: string;
  contactPhone?: string;
  contactEmail?: string;
  address?: string;
};

export default function ExhibitorProfilePage() {
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [editing, setEditing] = useState(false);
  const [exhibitor, setExhibitor] = useState<Exhibitor>();

  useEffect(() => {
    let alive = true;
    async function loadProfile() {
      setLoading(true);
      setLoadError(false);
      try {
        const loaded = await apiGet<Exhibitor>('/api/exhibitors/current');
        if (alive) setExhibitor(loaded);
      } catch {
        if (alive) setLoadError(true);
      } finally {
        if (alive) setLoading(false);
      }
    }
    loadProfile();
    return () => { alive = false; };
  }, []);

  const columns = useMemo<ProDescriptionsItemProps<Exhibitor>[]>(
    () => [
      { title: '企业名称', dataIndex: 'companyName' },
      { title: '统一社会信用代码', dataIndex: 'creditCode', renderText: (value) => value || '-' },
      { title: '联系人', dataIndex: 'contactName', renderText: (value) => value || '-' },
      { title: '联系电话', dataIndex: 'contactPhone', renderText: (value) => value || '-' },
      { title: '邮箱', dataIndex: 'contactEmail', renderText: (value) => value || '-' },
      { title: '地址', dataIndex: 'address', renderText: (value) => value || '-' },
      {
        title: '状态',
        dataIndex: 'status',
        render: (_, record) => <Tag color={record.status === 'ACTIVE' ? 'green' : 'default'}>{formatExhibitorStatus(record.status)}</Tag>,
      },
    ],
    [],
  );

  if (loading) {
    return (
      <PageContainer title="企业资料">
        <Skeleton active paragraph={{ rows: 8 }} />
      </PageContainer>
    );
  }

  if (loadError || !exhibitor) {
    return (
      <PageContainer title="企业资料">
        <ProCard>
          <Result
            status="error"
            title="企业资料加载失败"
            subTitle="请检查网络或稍后重试。"
            extra={<Button type="primary" onClick={() => window.location.reload()}>重新加载</Button>}
          />
        </ProCard>
      </PageContainer>
    );
  }

  if (editing) {
    return (
      <PageContainer title="企业资料">
        <ProCard>
          <ProForm<ProfileValues>
            layout="vertical"
            initialValues={exhibitor}
            submitter={{
              searchConfig: { submitText: '保存资料', resetText: '取消' },
              onReset: () => setEditing(false),
            }}
            onFinish={async (values) => {
              const updated = await apiPut<Exhibitor, ProfileValues>('/api/exhibitors/current', values);
              setExhibitor(updated);
              setEditing(false);
              message.success('企业资料已保存');
              return true;
            }}
          >
            <ProFormText name="companyName" label="企业名称" rules={[{ required: true, message: '请输入企业名称' }]} />
            <ProFormText name="creditCode" label="统一社会信用代码" />
            <ProFormText name="contactName" label="联系人" />
            <ProFormText name="contactPhone" label="联系电话" />
            <ProFormText name="contactEmail" label="邮箱" />
            <ProFormTextArea name="address" label="地址" fieldProps={{ rows: 3, maxLength: 300 }} />
          </ProForm>
        </ProCard>
      </PageContainer>
    );
  }

  return (
    <PageContainer
      title="企业资料"
      extra={<Button type="primary" onClick={() => setEditing(true)}>编辑资料</Button>}
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <ProCard>
          <ProDescriptions<Exhibitor> column={1} dataSource={exhibitor} columns={columns} />
        </ProCard>
      </Space>
    </PageContainer>
  );
}
