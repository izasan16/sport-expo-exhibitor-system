import { PageContainer, ProCard } from '@ant-design/pro-components';
import { Button, Empty, List, message, Result, Segmented, Skeleton, Space, Tag, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { DEMO_EXHIBITOR_ID } from '../../../app/demoActors';
import { apiGet, apiPost } from '../../../app/request';
import {
  formatMobileText,
  formatMobileTime,
  formatNotificationType,
  getNotificationTarget,
} from '../mobileViewModels';
import { filterNotificationsByReadState, getExhibitorLoadErrorCopy } from '../pageLogic';

type NotificationRecord = {
  id: string;
  eventId?: string | null;
  recipientType?: string | null;
  recipientId?: string | null;
  title?: string | null;
  content?: string | null;
  type?: string | null;
  relatedType?: string | null;
  relatedId?: string | null;
  relatedOrderId?: string | null;
  readAt?: string | null;
  createdAt?: string | null;
};

type NotificationFilter = 'all' | 'unread';

export default function NotificationCenterPage() {
  const [loading, setLoading] = useState(true);
  const [markingId, setMarkingId] = useState<string>();
  const [filter, setFilter] = useState<NotificationFilter>('all');
  const [notifications, setNotifications] = useState<NotificationRecord[]>([]);
  const [loadError, setLoadError] = useState(false);
  const [reloadSignal, setReloadSignal] = useState(0);
  const navigate = useNavigate();

  useEffect(() => {
    let alive = true;

    async function loadNotifications() {
      setLoading(true);
      setLoadError(false);
      try {
        const data = await apiGet<NotificationRecord[]>(
          `/api/notifications?recipientId=${DEMO_EXHIBITOR_ID}`,
        );
        if (!alive) return;
        setNotifications(data);
      } catch {
        if (!alive) return;
        setNotifications([]);
        setLoadError(true);
      } finally {
        if (alive) setLoading(false);
      }
    }

    loadNotifications();
    return () => {
      alive = false;
    };
  }, [reloadSignal]);

  const filteredNotifications = useMemo(
    () => filterNotificationsByReadState(notifications, filter),
    [filter, notifications],
  );

  if (!loading && loadError) {
    const copy = getExhibitorLoadErrorCopy('notifications');
    return (
      <PageContainer title="通知中心">
        <ProCard>
          <Result
            status="error"
            title={copy.title}
            subTitle={copy.subTitle}
            extra={<Button type="primary" onClick={() => setReloadSignal((current) => current + 1)}>{copy.actionText}</Button>}
          />
        </ProCard>
      </PageContainer>
    );
  }

  async function markRead(notification: NotificationRecord) {
    if (notification.readAt) return;

    setMarkingId(notification.id);
    try {
      const updated = await apiPost<NotificationRecord>(
        `/api/notifications/${notification.id}/read?recipientId=${DEMO_EXHIBITOR_ID}`,
      );
      setNotifications((current) => current.map((item) => (item.id === updated.id ? updated : item)));
      message.success('已标记为已读');
    } finally {
      setMarkingId(undefined);
    }
  }

  return (
    <PageContainer title="通知中心">
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Segmented
          value={filter}
          onChange={(value) => setFilter(value as NotificationFilter)}
          options={[
            { label: '全部', value: 'all' },
            { label: '未读', value: 'unread' },
          ]}
        />

        {loading ? (
          <Skeleton active paragraph={{ rows: 8 }} />
        ) : (
          <List<NotificationRecord>
            dataSource={filteredNotifications}
            locale={{ emptyText: <Empty description="暂无" /> }}
            renderItem={(notification) => {
              const isRead = Boolean(notification.readAt);
              const target = getNotificationTarget(notification);
              return (
                <List.Item style={{ paddingInline: 0 }}>
                  <ProCard
                    title={formatMobileText(notification.title)}
                    extra={<Tag color={isRead ? 'default' : 'blue'}>{isRead ? '已读' : '未读'}</Tag>}
                    onClick={() => {
                      if (target) navigate(target);
                    }}
                    style={{ cursor: target ? 'pointer' : undefined }}
                  >
                    <Space direction="vertical" size={8} style={{ width: '100%' }}>
                      <Typography.Text>{formatMobileText(notification.content)}</Typography.Text>
                      <Typography.Text type="secondary">
                        类型：{notification.type ? formatNotificationType(notification.type) : '未填写'}
                      </Typography.Text>
                      <Typography.Text type="secondary">
                        创建时间：{formatMobileTime(notification.createdAt)}
                      </Typography.Text>
                      <Typography.Text type="secondary">
                        阅读时间：{notification.readAt ? formatMobileTime(notification.readAt) : '未读'}
                      </Typography.Text>
                      {target ? (
                        <Button
                          size="small"
                          onClick={(event) => {
                            event.stopPropagation();
                            navigate(target);
                          }}
                        >
                          查看详情
                        </Button>
                      ) : null}
                      <Button
                        type="primary"
                        size="small"
                        disabled={isRead}
                        loading={markingId === notification.id}
                        onClick={(event) => {
                          event.stopPropagation();
                          markRead(notification);
                        }}
                      >
                        标记已读
                      </Button>
                    </Space>
                  </ProCard>
                </List.Item>
              );
            }}
          />
        )}
      </Space>
    </PageContainer>
  );
}
