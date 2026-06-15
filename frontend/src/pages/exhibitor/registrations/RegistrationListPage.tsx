import { PageContainer, ProCard } from '@ant-design/pro-components';
import { Button, Empty, List, Result, Skeleton, Space, Tag, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { DEMO_EXHIBITOR_ID } from '../../../app/demoActors';
import { formatEventDisplay, formatRegistrationStatus } from '../../../app/display';
import type { Registration } from '../../../app/domain';
import { formatArea, formatFriendlyTime } from '../../../app/formatters';
import { apiGet } from '../../../app/request';
import { useEvents } from '../../../app/useEvents';
import { getRegistrationListPrimaryAction } from '../registrationViewModels';

function getRegistrationStatusColor(status: Registration['status']) {
  if (status === 'APPROVED') return 'green';
  if (status === 'REJECTED') return 'red';
  if (status === 'SUBMITTED') return 'blue';
  if (status === 'CANCELLED') return 'default';
  return 'gold';
}

export default function RegistrationListPage() {
  const navigate = useNavigate();
  const { events } = useEvents();
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [registrations, setRegistrations] = useState<Registration[]>([]);

  useEffect(() => {
    let alive = true;
    async function loadRegistrations() {
      setLoading(true);
      setLoadError(false);
      try {
        const loaded = await apiGet<Registration[]>(`/api/registrations?exhibitorId=${DEMO_EXHIBITOR_ID}`);
        if (alive) setRegistrations(loaded);
      } catch {
        if (alive) {
          setRegistrations([]);
          setLoadError(true);
        }
      } finally {
        if (alive) setLoading(false);
      }
    }
    loadRegistrations();
    return () => { alive = false; };
  }, []);

  const sortedRegistrations = useMemo(
    () => [...registrations].sort((left, right) => (Date.parse(right.updatedAt) || 0) - (Date.parse(left.updatedAt) || 0)),
    [registrations],
  );
  const primaryAction = getRegistrationListPrimaryAction(sortedRegistrations);

  if (loading) {
    return (
      <PageContainer title="报名进度">
        <Skeleton active paragraph={{ rows: 8 }} />
      </PageContainer>
    );
  }

  if (loadError) {
    return (
      <PageContainer title="报名进度">
        <ProCard>
          <Result
            status="error"
            title="报名数据加载失败"
            subTitle="请检查网络或稍后重试。"
            extra={<Button type="primary" onClick={() => window.location.reload()}>重新加载</Button>}
          />
        </ProCard>
      </PageContainer>
    );
  }

  return (
    <PageContainer
      title="报名进度"
      extra={<Button type="primary" onClick={() => navigate(primaryAction.to)}>{primaryAction.label}</Button>}
    >
      <ProCard>
        {sortedRegistrations.length ? (
          <List
            dataSource={sortedRegistrations}
            renderItem={(registration) => (
              <List.Item style={{ paddingInline: 0 }}>
                <ProCard
                  hoverable
                  onClick={() => navigate(`/exhibitor/registrations/${registration.id}`)}
                  style={{ width: '100%' }}
                >
                  <Space direction="vertical" size={8} style={{ width: '100%' }}>
                    <Space style={{ justifyContent: 'space-between', width: '100%' }} align="start">
                      <Typography.Text strong>{formatEventDisplay(registration.eventId, events)}</Typography.Text>
                      <Tag color={getRegistrationStatusColor(registration.status)}>{formatRegistrationStatus(registration.status)}</Tag>
                    </Space>
                    <Typography.Text type="secondary">
                      {registration.categoryName || registration.category || '未选择类别'}
                      {registration.intentAreaSqm ? ` · 意向 ${formatArea(registration.intentAreaSqm)}` : ''}
                    </Typography.Text>
                    <Typography.Text type="secondary">
                      更新 {formatFriendlyTime(registration.updatedAt)}
                    </Typography.Text>
                  </Space>
                </ProCard>
              </List.Item>
            )}
          />
        ) : (
          <Empty description="还没有报名记录">
            <Button type="primary" onClick={() => navigate(primaryAction.to)}>{primaryAction.label}</Button>
          </Empty>
        )}
      </ProCard>
    </PageContainer>
  );
}
