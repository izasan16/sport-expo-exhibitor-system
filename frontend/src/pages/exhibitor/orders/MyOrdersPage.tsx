import { Button, Card, Empty, List, message, Result, Skeleton, Space, Typography } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { DEMO_EXHIBITOR_ID } from '../../../app/demoActors';
import { formatBoothType, formatEventDisplay, formatExhibitorDisplay } from '../../../app/display';
import type { ParticipationOrder } from '../../../app/domain';
import { formatArea, formatShortId } from '../../../app/formatters';
import { apiGet } from '../../../app/request';
import { getOrderStatusMeta, StatusTag } from '../../../app/status';
import { useEvents } from '../../../app/useEvents';
import { formatMobileAmount, formatMobileText, formatMobileTime } from '../mobileViewModels';
import { getExhibitorLoadErrorCopy, getOrderCardInteraction, getOrderNextAction } from '../pageLogic';

export default function MyOrdersPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const messageShownRef = useRef(false);
  const { events } = useEvents();
  const [orders, setOrders] = useState<ParticipationOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [reloadSignal, setReloadSignal] = useState(0);

  useEffect(() => {
    const state = location.state as { message?: string } | null;
    if (!messageShownRef.current && state?.message) {
      messageShownRef.current = true;
      message.info(state.message);
    }
  }, [location.state]);

  useEffect(() => {
    let alive = true;

    async function loadOrders() {
      setLoading(true);
      setLoadError(false);
      try {
        const data = await apiGet<ParticipationOrder[]>(`/api/participation-orders?exhibitorId=${DEMO_EXHIBITOR_ID}`);
        if (!alive) return;
        setOrders(data);
      } catch {
        if (!alive) return;
        setOrders([]);
        setLoadError(true);
      } finally {
        if (alive) setLoading(false);
      }
    }

    loadOrders();
    return () => {
      alive = false;
    };
  }, [reloadSignal]);

  if (!loading && loadError) {
    const copy = getExhibitorLoadErrorCopy('orders');
    return (
      <div className="exhibitor-page">
        <Card>
          <Result
            status="error"
            title={copy.title}
            subTitle={copy.subTitle}
            extra={<Button type="primary" onClick={() => setReloadSignal((current) => current + 1)}>{copy.actionText}</Button>}
          />
        </Card>
      </div>
    );
  }

  return (
    <div className="exhibitor-page">
      <div className="exhibitor-page-heading">
        <Typography.Title level={4}>我的订单</Typography.Title>
        <Typography.Text type="secondary">点按订单卡片查看进度、合同、付款和选位。</Typography.Text>
      </div>
      {loading ? (
        <Skeleton active paragraph={{ rows: 8 }} />
      ) : (
        <List<ParticipationOrder>
          dataSource={orders}
          locale={{ emptyText: <Empty description="暂无参展订单" /> }}
          renderItem={(record) => {
            const nextAction = getOrderNextAction({ order: record });
            const cardInteraction = getOrderCardInteraction(record);
            const eventDisplay = record.eventId ? formatEventDisplay(record.eventId, events) : '未填写';
            const exhibitorDisplay = record.exhibitorId ? formatExhibitorDisplay(record.exhibitorId) : '未填写';
            const statusMeta = record.status ? getOrderStatusMeta(record.status) : { label: '未填写', color: 'default' };

            return (
              <List.Item style={{ paddingInline: 0 }}>
                <Card
                  className="exhibitor-order-card"
                  role="button"
                  tabIndex={0}
                  onClick={() => navigate(cardInteraction.tapTarget)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault();
                      navigate(cardInteraction.tapTarget);
                    }
                  }}
                  style={{ width: '100%' }}
                  bodyStyle={{ padding: 16 }}
                >
                  <Space direction="vertical" size={12} style={{ width: '100%' }}>
                    <Space align="start" style={{ width: '100%', justifyContent: 'space-between' }}>
                      <Space direction="vertical" size={4}>
                        <Typography.Text strong>{formatMobileText(eventDisplay)}</Typography.Text>
                        <Typography.Text type="secondary">订单 {formatShortId(record.id)}</Typography.Text>
                      </Space>
                      <StatusTag meta={statusMeta} />
                    </Space>
                    <div className="exhibitor-order-card-main">
                      <Typography.Text className="exhibitor-order-amount">{formatMobileAmount(record.totalAmount)}</Typography.Text>
                      <Typography.Text type="secondary">
                        {formatArea(record.areaSqm)} · {formatBoothType(record.boothType)}
                      </Typography.Text>
                    </div>
                    <Space direction="vertical" size={2}>
                      <Typography.Text type="secondary">展商：{exhibitorDisplay}</Typography.Text>
                      <Typography.Text type="secondary">创建：{formatMobileTime(record.createdAt)}</Typography.Text>
                    </Space>
                    <div className="exhibitor-card-footer">
                      <Typography.Text>{nextAction.disabled ? nextAction.reason || nextAction.label : `下一步：${nextAction.label}`}</Typography.Text>
                      <Typography.Text type="secondary">点按查看</Typography.Text>
                    </div>
                  </Space>
                </Card>
              </List.Item>
            );
          }}
        />
      )}
    </div>
  );
}
