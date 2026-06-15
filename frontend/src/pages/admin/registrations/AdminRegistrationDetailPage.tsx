import { ModalForm, PageContainer, ProCard, ProDescriptions, ProFormTextArea } from '@ant-design/pro-components';
import type { ProDescriptionsItemProps } from '@ant-design/pro-components';
import { Button, message, Modal, Result, Skeleton, Space, Tag } from 'antd';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { notifyAdminDashboardStale } from '../../../app/apiLoaders';
import { formatEventDisplay, formatRegistrationStatus } from '../../../app/display';
import type { Exhibitor, Registration } from '../../../app/domain';
import { formatArea, formatFriendlyTime, formatShortId } from '../../../app/formatters';
import { apiGet, apiPost } from '../../../app/request';
import { useEvents } from '../../../app/useEvents';

const rejectReasonMaxLength = 500;

function getRegistrationStatusColor(status: Registration['status']) {
  if (status === 'APPROVED') return 'green';
  if (status === 'REJECTED') return 'red';
  if (status === 'SUBMITTED') return 'blue';
  if (status === 'CANCELLED') return 'default';
  return 'gold';
}

function formatRegistrationCategory(record: Registration) {
  if (record.categoryName && record.category) return `${record.categoryName}（${record.category}）`;
  return record.categoryName || record.category || '-';
}

export default function AdminRegistrationDetailPage() {
  const { registrationId } = useParams();
  const navigate = useNavigate();
  const requestSeq = useRef(0);
  const { events } = useEvents();
  const [loading, setLoading] = useState(true);
  const [registration, setRegistration] = useState<Registration>();
  const [exhibitor, setExhibitor] = useState<Exhibitor>();
  const [reviewing, setReviewing] = useState(false);

  const loadRegistration = useCallback(async () => {
    const requestId = requestSeq.current + 1;
    requestSeq.current = requestId;

    if (!registrationId) {
      setRegistration(undefined);
      setExhibitor(undefined);
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      const loaded = await apiGet<Registration>(`/api/registrations/${encodeURIComponent(registrationId)}`);
      if (requestSeq.current !== requestId) return;
      setRegistration(loaded);

      try {
        const loadedExhibitor = await apiGet<Exhibitor>(`/api/exhibitors/${encodeURIComponent(loaded.exhibitorId)}`);
        if (requestSeq.current === requestId) setExhibitor(loadedExhibitor);
      } catch {
        if (requestSeq.current === requestId) setExhibitor(undefined);
      }
    } catch {
      if (requestSeq.current !== requestId) return;
      setRegistration(undefined);
      setExhibitor(undefined);
    } finally {
      if (requestSeq.current === requestId) setLoading(false);
    }
  }, [registrationId]);

  useEffect(() => {
    loadRegistration();
    return () => {
      requestSeq.current += 1;
    };
  }, [loadRegistration]);

  const columns = useMemo<ProDescriptionsItemProps<Registration>[]>(
    () => [
      { title: '报名 ID', dataIndex: 'id', copyable: true, ellipsis: true },
      { title: '展会', dataIndex: 'eventId', render: (_, record) => formatEventDisplay(record.eventId, events) },
      { title: '展商', dataIndex: 'exhibitorId', renderText: (value) => exhibitor?.companyName || `展商 ${formatShortId(value)}` },
      { title: '报名类别', dataIndex: 'category', render: (_, record) => formatRegistrationCategory(record) },
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
      { title: '审核人', dataIndex: 'reviewedBy', renderText: (value) => value || '-' },
      { title: '审核时间', dataIndex: 'reviewedAt', renderText: (value) => (value ? formatFriendlyTime(value) : '-') },
      { title: '提交时间', dataIndex: 'submittedAt', renderText: (value) => (value ? formatFriendlyTime(value) : '-') },
      { title: '更新时间', dataIndex: 'updatedAt', renderText: (value) => formatFriendlyTime(value) },
    ],
    [events, exhibitor?.companyName],
  );

  const approve = () => {
    if (!registration) return;
    Modal.confirm({
      title: '确认报名审核通过？',
      content: `报名 ${formatShortId(registration.id)} 通过后，展商可基于报名创建订单。`,
      okText: '审核通过',
      cancelText: '取消',
      onOk: async () => {
        setReviewing(true);
        try {
          const approved = await apiPost<Registration>(`/api/registrations/${registration.id}/approve`);
          setRegistration(approved);
          message.success('报名审核已通过');
          notifyAdminDashboardStale();
        } finally {
          setReviewing(false);
        }
      },
    });
  };

  if (loading) {
    return (
      <PageContainer title="报名详情">
        <Skeleton active paragraph={{ rows: 10 }} />
      </PageContainer>
    );
  }

  if (!registration) {
    return (
      <PageContainer title="报名详情">
        <ProCard>
          <Result
            status="404"
            title="未找到报名"
            subTitle={`报名 ${registrationId ?? '-'} 不存在或暂时无法访问。`}
            extra={
              <Button type="primary" onClick={() => navigate('/admin/registrations')}>
                返回报名审核
              </Button>
            }
          />
        </ProCard>
      </PageContainer>
    );
  }

  return (
    <PageContainer
      title={`报名 ${formatShortId(registration.id)}`}
      extra={
        registration.status === 'SUBMITTED'
          ? [
              <Button key="approve" type="primary" loading={reviewing} onClick={approve}>
                审核通过
              </Button>,
              <ModalForm
                key="reject"
                title="驳回报名"
                trigger={
                  <Button danger loading={reviewing}>
                    驳回
                  </Button>
                }
                modalProps={{ destroyOnClose: true }}
                submitter={{ submitButtonProps: { loading: reviewing } }}
                onFinish={async (values) => {
                  setReviewing(true);
                  try {
                    const rejected = await apiPost<Registration, { reason: string }>(
                      `/api/registrations/${registration.id}/reject`,
                      { reason: values.reason },
                    );
                    setRegistration(rejected);
                    message.success('报名已驳回');
                    notifyAdminDashboardStale();
                    return true;
                  } finally {
                    setReviewing(false);
                  }
                }}
              >
                <ProFormTextArea
                  name="reason"
                  label="驳回原因"
                  rules={[
                    { required: true, message: '请输入驳回原因' },
                    { max: rejectReasonMaxLength, message: '驳回原因不能超过 500 字' },
                  ]}
                  fieldProps={{ showCount: true, maxLength: rejectReasonMaxLength, rows: 4 }}
                />
              </ModalForm>,
            ]
          : []
      }
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <ProCard>
          <ProDescriptions<Registration> column={2} dataSource={registration} columns={columns} />
        </ProCard>
      </Space>
    </PageContainer>
  );
}
