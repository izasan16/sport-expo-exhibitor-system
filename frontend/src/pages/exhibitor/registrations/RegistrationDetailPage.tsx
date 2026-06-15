import {
  PageContainer,
  ProCard,
  ProDescriptions,
  ProForm,
  ProFormDigit,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
} from '@ant-design/pro-components';
import type { ProDescriptionsItemProps, ProFormInstance } from '@ant-design/pro-components';
import { Button, Empty, message, Result, Skeleton, Space, Tag } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { DEMO_EXHIBITOR_ID } from '../../../app/demoActors';
import { formatEventCategoryDisplay, formatEventDisplay, formatRegistrationStatus } from '../../../app/display';
import type { EventCategory, Registration } from '../../../app/domain';
import { formatArea, formatFriendlyTime } from '../../../app/formatters';
import { apiGet, apiPost, apiPut } from '../../../app/request';
import { useEvents } from '../../../app/useEvents';
import { shouldUseMobileExhibitorView } from '../pageLogic';

type RegistrationEditValues = {
  category: string;
  intentAreaSqm?: number;
  contactName?: string;
  contactPhone?: string;
  remark?: string;
};

function getRegistrationStatusColor(status: Registration['status']) {
  if (status === 'APPROVED') return 'green';
  if (status === 'REJECTED') return 'red';
  if (status === 'SUBMITTED') return 'blue';
  if (status === 'CANCELLED') return 'default';
  return 'gold';
}

export default function RegistrationDetailPage() {
  const navigate = useNavigate();
  const { registrationId } = useParams();
  const { events } = useEvents();
  const isMobile = shouldUseMobileExhibitorView();
  const formRef = useRef<ProFormInstance<RegistrationEditValues>>();
  const [loading, setLoading] = useState(true);
  const [loadingCategories, setLoadingCategories] = useState(false);
  const [loadError, setLoadError] = useState(false);
  const [registration, setRegistration] = useState<Registration>();
  const [categories, setCategories] = useState<EventCategory[]>([]);

  useEffect(() => {
    let alive = true;
    async function loadRegistration() {
      if (!registrationId) return;
      setLoading(true);
      setLoadError(false);
      try {
        const loaded = await apiGet<Registration>(`/api/registrations/${registrationId}`);
        if (alive) setRegistration(loaded);
      } catch {
        if (alive) setLoadError(true);
      } finally {
        if (alive) setLoading(false);
      }
    }
    loadRegistration();
    return () => { alive = false; };
  }, [registrationId]);

  useEffect(() => {
    let alive = true;
    async function loadCategories() {
      if (!registration?.eventId || registration.status !== 'REJECTED') {
        setCategories([]);
        return;
      }
      setLoadingCategories(true);
      try {
        const loaded = await apiGet<EventCategory[]>(
          `/api/event-categories?eventId=${encodeURIComponent(registration.eventId)}&enabled=true`,
        );
        if (alive) setCategories(loaded);
      } catch {
        if (alive) {
          setCategories([]);
          message.error('报名类别加载失败，请稍后重试');
        }
      } finally {
        if (alive) setLoadingCategories(false);
      }
    }
    loadCategories();
    return () => { alive = false; };
  }, [registration?.eventId, registration?.status]);

  const columns = useMemo<ProDescriptionsItemProps<Registration>[]>(
    () => [
      { title: '展会', dataIndex: 'eventId', renderText: (value) => formatEventDisplay(value, events) },
      { title: '报名类别', dataIndex: 'categoryName', render: (_, record) => record.categoryName || record.category || '-' },
      {
        title: '状态',
        dataIndex: 'status',
        render: (_, record) => (
          <Tag color={getRegistrationStatusColor(record.status)}>{formatRegistrationStatus(record.status)}</Tag>
        ),
      },
      { title: '意向面积', dataIndex: 'intentAreaSqm', renderText: (value) => (value ? formatArea(value) : '-') },
      { title: '联系人', dataIndex: 'contactName', renderText: (value) => value || '-' },
      { title: '联系电话', dataIndex: 'contactPhone', renderText: (value) => value || '-' },
      { title: '备注', dataIndex: 'remark', renderText: (value) => value || '-' },
      { title: '审核意见', dataIndex: 'reviewComment', renderText: (value) => value || '-' },
      { title: '提交时间', dataIndex: 'submittedAt', renderText: (value) => (value ? formatFriendlyTime(value) : '-') },
      { title: '更新时间', dataIndex: 'updatedAt', renderText: (value) => formatFriendlyTime(value) },
    ],
    [events],
  );

  const categoryOptions = useMemo(
    () => [...categories]
      .sort((left, right) => left.sortOrder - right.sortOrder)
      .map((category) => ({ label: formatEventCategoryDisplay(category), value: category.code })),
    [categories],
  );

  if (loading) {
    return (
      <PageContainer title="报名详情">
        <Skeleton active paragraph={{ rows: 8 }} />
      </PageContainer>
    );
  }

  if (loadError) {
    return (
      <PageContainer title="报名详情">
        <ProCard>
          <Result
            status="error"
            title="报名详情加载失败"
            subTitle="请检查网络或稍后重试。"
            extra={<Button type="primary" onClick={() => window.location.reload()}>重新加载</Button>}
          />
        </ProCard>
      </PageContainer>
    );
  }

  if (!registration) {
    return (
      <PageContainer title="报名详情">
        <ProCard>
          <Empty description="未找到报名记录">
            <Button onClick={() => navigate('/exhibitor/registrations')}>返回报名进度</Button>
          </Empty>
        </ProCard>
      </PageContainer>
    );
  }

  return (
    <PageContainer
      title="报名详情"
      extra={
        registration.status === 'APPROVED'
          ? <Button type="primary" onClick={() => navigate('/exhibitor/orders/new')}>去下单</Button>
          : undefined
      }
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <ProCard>
          <ProDescriptions<Registration> column={1} dataSource={registration} columns={columns} />
        </ProCard>

        {registration.status === 'REJECTED' ? (
          <ProCard title="修改后重新提交">
            <ProForm<RegistrationEditValues>
              formRef={formRef}
              layout={isMobile ? 'vertical' : 'horizontal'}
              initialValues={{
                category: registration.category ?? undefined,
                intentAreaSqm: registration.intentAreaSqm === null || registration.intentAreaSqm === undefined
                  ? undefined
                  : Number(registration.intentAreaSqm),
                contactName: registration.contactName ?? undefined,
                contactPhone: registration.contactPhone ?? undefined,
                remark: registration.remark ?? undefined,
              }}
              submitter={{
                render: () => [
                  <Button key="save" onClick={async () => {
                    const values = await formRef.current?.validateFields();
                    if (!values) return;
                    const selectedCategory = categories.find((category) => category.code === values.category);
                    const updated = await apiPut<Registration, Partial<Registration>>(`/api/registrations/${registration.id}`, {
                      eventId: registration.eventId,
                      exhibitorId: DEMO_EXHIBITOR_ID,
                      category: values.category,
                      categoryName: selectedCategory?.name,
                      intentAreaSqm: values.intentAreaSqm,
                      contactName: values.contactName,
                      contactPhone: values.contactPhone,
                      remark: values.remark,
                    });
                    setRegistration(updated);
                    message.success('报名信息已保存');
                  }}>
                    保存
                  </Button>,
                  <Button key="submit" type="primary" onClick={async () => {
                    const values = await formRef.current?.validateFields();
                    if (!values) return;
                    const selectedCategory = categories.find((category) => category.code === values.category);
                    const updated = await apiPut<Registration, Partial<Registration>>(`/api/registrations/${registration.id}`, {
                      eventId: registration.eventId,
                      exhibitorId: DEMO_EXHIBITOR_ID,
                      category: values.category,
                      categoryName: selectedCategory?.name,
                      intentAreaSqm: values.intentAreaSqm,
                      contactName: values.contactName,
                      contactPhone: values.contactPhone,
                      remark: values.remark,
                    });
                    const submitted = await apiPost<Registration>(`/api/registrations/${updated.id}/submit`);
                    setRegistration(submitted);
                    message.success('报名已重新提交');
                  }}>
                    重新提交
                  </Button>,
                ],
              }}
            >
              <ProFormSelect
                name="category"
                label="报名类别"
                rules={[{ required: true, message: '请选择报名类别' }]}
                options={categoryOptions}
                fieldProps={{ loading: loadingCategories }}
                disabled={loadingCategories || categoryOptions.length === 0}
              />
              <ProFormDigit name="intentAreaSqm" label="意向面积（㎡）" min={1} fieldProps={{ precision: 2 }} />
              <ProFormText name="contactName" label="报名联系人" />
              <ProFormText name="contactPhone" label="联系电话" />
              <ProFormTextArea name="remark" label="备注" fieldProps={{ rows: 3, maxLength: 500 }} />
            </ProForm>
          </ProCard>
        ) : null}
      </Space>
    </PageContainer>
  );
}
