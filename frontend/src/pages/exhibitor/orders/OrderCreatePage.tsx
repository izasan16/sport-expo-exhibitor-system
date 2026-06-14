import { PageContainer, ProCard, ProForm, ProFormSelect } from '@ant-design/pro-components';
import type { ProFormInstance } from '@ant-design/pro-components';
import { Button, Empty, message, Result, Skeleton, Space } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiGet, apiPost } from '../../../app/request';
import { useIsMobile } from '../../../app/responsive';
import { buildAreaTierOptions, buildOrderCreateBody, getExhibitorLoadErrorCopy, type BoothType } from '../pageLogic';

type OrderCreateValues = {
  eventId: string;
  areaTierId: string;
  boothType: BoothType;
  zoneId?: string;
};

type EventOption = {
  id: string;
  name: string;
  year: number;
  status: string;
};

type AreaTierOption = {
  id: string;
  eventId: string;
  zoneId?: string | null;
  areaSqm: number | string;
  enabled: boolean;
};

type CreatedOrder = {
  id: string;
  status: string;
  totalAmount: number | string;
};

export default function OrderCreatePage() {
  const navigate = useNavigate();
  const isMobile = useIsMobile();
  const formRef = useRef<ProFormInstance<OrderCreateValues>>();
  const [loading, setLoading] = useState(true);
  const [loadingTiers, setLoadingTiers] = useState(false);
  const [loadError, setLoadError] = useState(false);
  const [events, setEvents] = useState<EventOption[]>([]);
  const [areaTiers, setAreaTiers] = useState<AreaTierOption[]>([]);
  const [selectedEventId, setSelectedEventId] = useState<string>();

  useEffect(() => {
    let alive = true;

    async function loadEvents() {
      setLoading(true);
      setLoadError(false);
      try {
        const loadedEvents = await apiGet<EventOption[]>('/api/events');
        if (!alive) return;
        setEvents(loadedEvents);
        const firstEvent = loadedEvents[0]?.id;
        setSelectedEventId(firstEvent);
        if (firstEvent) {
          formRef.current?.setFieldsValue({ eventId: firstEvent });
        }
      } catch {
        if (!alive) return;
        setEvents([]);
        setLoadError(true);
      } finally {
        if (alive) setLoading(false);
      }
    }

    loadEvents();
    return () => {
      alive = false;
    };
  }, []);

  useEffect(() => {
    let alive = true;

    async function loadAreaTiers() {
      if (!selectedEventId) {
        setAreaTiers([]);
        return;
      }

      setLoadingTiers(true);
      try {
        const loadedTiers = await apiGet<AreaTierOption[]>(`/api/area-tiers?eventId=${encodeURIComponent(selectedEventId)}`);
        if (!alive) return;
        setAreaTiers(loadedTiers);
      } catch {
        if (!alive) return;
        setAreaTiers([]);
        message.error('面积档位加载失败，请稍后重试');
      } finally {
        if (alive) setLoadingTiers(false);
      }
    }

    formRef.current?.setFieldsValue({ areaTierId: undefined });
    loadAreaTiers();
    return () => {
      alive = false;
    };
  }, [selectedEventId]);

  const eventOptions = useMemo(
    () => events.map((event) => ({ label: `${event.name}（${event.year}）`, value: event.id })),
    [events],
  );
  const areaTierOptions = useMemo(() => buildAreaTierOptions(areaTiers, selectedEventId), [areaTiers, selectedEventId]);

  if (loading) {
    return (
      <PageContainer title="我要下单">
        <Skeleton active paragraph={{ rows: 8 }} />
      </PageContainer>
    );
  }

  if (loadError) {
    const copy = getExhibitorLoadErrorCopy('orderCreate');
    return (
      <PageContainer title="我要下单">
        <ProCard>
          <Result
            status="error"
            title={copy.title}
            subTitle={copy.subTitle}
            extra={<Button type="primary" onClick={() => window.location.reload()}>{copy.actionText}</Button>}
          />
        </ProCard>
      </PageContainer>
    );
  }

  if (!events.length) {
    return (
      <PageContainer title="我要下单">
        <ProCard>
          <Empty description="当前没有可报名展会">
            <Button type="primary" onClick={() => navigate('/exhibitor/orders')}>返回我的订单</Button>
          </Empty>
        </ProCard>
      </PageContainer>
    );
  }

  return (
    <PageContainer title="我要下单">
      <ProForm<OrderCreateValues>
        formRef={formRef}
        layout={isMobile ? 'vertical' : 'horizontal'}
        initialValues={{ eventId: selectedEventId, boothType: 'STANDARD' }}
        submitter={{
          searchConfig: { submitText: '提交订单' },
          render: (_, doms) => (
            isMobile
              ? [<Button key="submit" type="primary" htmlType="submit" block>提交订单</Button>]
              : doms
          ),
        }}
        onFinish={async (values) => {
          const selectedTier = areaTiers.find((tier) => tier.id === values.areaTierId);
          const created = await apiPost<CreatedOrder>('/api/participation-orders', buildOrderCreateBody({
            ...values,
            zoneId: selectedTier?.zoneId ?? undefined,
          }));
          message.success(`订单 ${created.id} 创建成功，状态：${created.status}，总金额：${created.totalAmount}`);
          navigate(`/exhibitor/orders/${created.id}`);
          return true;
        }}
      >
        <ProFormSelect
          name="eventId"
          label="展会"
          rules={[{ required: true, message: '请选择展会' }]}
          options={eventOptions}
          fieldProps={{
            onChange: (value) => setSelectedEventId(String(value)),
          }}
        />
        <ProFormSelect
          name="areaTierId"
          label="面积档位"
          rules={[{ required: true, message: '请选择面积档位' }]}
          options={areaTierOptions}
          fieldProps={{ loading: loadingTiers }}
          disabled={!selectedEventId || loadingTiers || areaTierOptions.length === 0}
          extra={
            <Space direction="vertical" size={4}>
              {!selectedEventId ? '请先选择展会' : null}
              {selectedEventId && !loadingTiers && areaTierOptions.length === 0 ? '当前展会还没有启用的面积档位，请联系销售或主办方管理员。' : null}
            </Space>
          }
        />
        <ProFormSelect
          name="boothType"
          label="展位类型"
          initialValue="STANDARD"
          rules={[{ required: true, message: '请选择展位类型' }]}
          options={[
            { label: 'STANDARD', value: 'STANDARD' },
            { label: 'CUSTOM', value: 'CUSTOM' },
          ]}
        />
      </ProForm>
    </PageContainer>
  );
}
