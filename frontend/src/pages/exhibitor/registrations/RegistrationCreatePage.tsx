import { PageContainer, ProCard, ProForm, ProFormDigit, ProFormSelect, ProFormText, ProFormTextArea } from '@ant-design/pro-components';
import type { ProFormInstance } from '@ant-design/pro-components';
import { Button, Empty, message, Result, Skeleton, Space } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { DEMO_EXHIBITOR_ID } from '../../../app/demoActors';
import { formatEventCategoryDisplay } from '../../../app/display';
import type { EventCategory, Registration } from '../../../app/domain';
import { apiGet, apiPost } from '../../../app/request';
import { shouldUseMobileExhibitorView } from '../pageLogic';

type EventOption = {
  id: string;
  name: string;
  year: number;
  status: string;
};

type RegistrationCreateValues = {
  eventId: string;
  category: string;
  intentAreaSqm?: number;
  contactName?: string;
  contactPhone?: string;
  remark?: string;
};

export default function RegistrationCreatePage() {
  const navigate = useNavigate();
  const isMobile = shouldUseMobileExhibitorView();
  const formRef = useRef<ProFormInstance<RegistrationCreateValues>>();
  const [loading, setLoading] = useState(true);
  const [loadingCategories, setLoadingCategories] = useState(false);
  const [loadError, setLoadError] = useState(false);
  const [events, setEvents] = useState<EventOption[]>([]);
  const [categories, setCategories] = useState<EventCategory[]>([]);
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
        const firstEventId = loadedEvents[0]?.id;
        setSelectedEventId(firstEventId);
        if (firstEventId) {
          formRef.current?.setFieldsValue({ eventId: firstEventId });
        }
      } catch {
        if (alive) setLoadError(true);
      } finally {
        if (alive) setLoading(false);
      }
    }
    loadEvents();
    return () => { alive = false; };
  }, []);

  useEffect(() => {
    let alive = true;
    async function loadCategories() {
      if (!selectedEventId) {
        setCategories([]);
        return;
      }
      setLoadingCategories(true);
      formRef.current?.setFieldsValue({ category: undefined });
      try {
        const loaded = await apiGet<EventCategory[]>(
          `/api/event-categories?eventId=${encodeURIComponent(selectedEventId)}&enabled=true`,
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
  }, [selectedEventId]);

  const eventOptions = useMemo(
    () => events.map((event) => ({ label: `${event.name}（${event.year}）`, value: event.id })),
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
      <PageContainer title="提交报名">
        <Skeleton active paragraph={{ rows: 8 }} />
      </PageContainer>
    );
  }

  if (loadError) {
    return (
      <PageContainer title="提交报名">
        <ProCard>
          <Result
            status="error"
            title="报名选项加载失败"
            subTitle="请检查网络或稍后重试。"
            extra={<Button type="primary" onClick={() => window.location.reload()}>重新加载</Button>}
          />
        </ProCard>
      </PageContainer>
    );
  }

  if (!events.length) {
    return (
      <PageContainer title="提交报名">
        <ProCard>
          <Empty description="当前没有可报名展会">
            <Button onClick={() => navigate('/exhibitor/registrations')}>返回报名进度</Button>
          </Empty>
        </ProCard>
      </PageContainer>
    );
  }

  return (
    <PageContainer title="提交报名">
      <ProCard>
        <ProForm<RegistrationCreateValues>
          formRef={formRef}
          layout={isMobile ? 'vertical' : 'horizontal'}
          initialValues={{ eventId: selectedEventId }}
          submitter={{
            searchConfig: { submitText: '提交报名' },
            render: (_, doms) => (
              isMobile
                ? [<Button key="submit" type="primary" htmlType="submit" block>提交报名</Button>]
                : doms
            ),
          }}
          onFinish={async (values) => {
            const selectedCategory = categories.find((category) => category.code === values.category);
            const created = await apiPost<Registration>('/api/registrations', {
              eventId: values.eventId,
              exhibitorId: DEMO_EXHIBITOR_ID,
              category: values.category,
              categoryName: selectedCategory?.name,
              intentAreaSqm: values.intentAreaSqm,
              contactName: values.contactName,
              contactPhone: values.contactPhone,
              remark: values.remark,
            });
            try {
              const submitted = await apiPost<Registration>(`/api/registrations/${created.id}/submit`);
              message.success('报名已提交，请等待主办方审核');
              navigate(`/exhibitor/registrations/${submitted.id}`);
            } catch {
              message.warning('报名草稿已保存，但提交失败，请进入详情页重新提交');
              navigate(`/exhibitor/registrations/${created.id}`);
            }
            return true;
          }}
        >
          <ProFormSelect
            name="eventId"
            label="展会"
            rules={[{ required: true, message: '请选择展会' }]}
            options={eventOptions}
            fieldProps={{ onChange: (value) => setSelectedEventId(String(value)) }}
          />
          <ProFormSelect
            name="category"
            label="报名类别"
            rules={[{ required: true, message: '请选择报名类别' }]}
            options={categoryOptions}
            fieldProps={{ loading: loadingCategories }}
            disabled={!selectedEventId || loadingCategories || categoryOptions.length === 0}
            extra={
              <Space direction="vertical" size={4}>
                {!selectedEventId ? '请先选择展会' : null}
                {selectedEventId && !loadingCategories && categoryOptions.length === 0 ? '当前展会没有启用的报名类别，请联系主办方。' : null}
              </Space>
            }
          />
          <ProFormDigit name="intentAreaSqm" label="意向面积（㎡）" min={1} fieldProps={{ precision: 2 }} />
          <ProFormText name="contactName" label="报名联系人" />
          <ProFormText name="contactPhone" label="联系电话" />
          <ProFormTextArea name="remark" label="备注" fieldProps={{ rows: 3, maxLength: 500 }} />
        </ProForm>
      </ProCard>
    </PageContainer>
  );
}
