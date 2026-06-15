import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Tag } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { buildValueEnum, formatEventDisplay, formatRegistrationStatus } from '../../../app/display';
import type { Exhibitor, Registration } from '../../../app/domain';
import { formatFriendlyTime, formatShortId } from '../../../app/formatters';
import { apiGet } from '../../../app/request';
import { useEvents } from '../../../app/useEvents';

const registrationStatuses: Registration['status'][] = ['DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'CANCELLED'];

function getRegistrationStatusColor(status: Registration['status']) {
  if (status === 'APPROVED') return 'green';
  if (status === 'REJECTED') return 'red';
  if (status === 'SUBMITTED') return 'blue';
  if (status === 'CANCELLED') return 'default';
  return 'gold';
}

function buildQuery(params: Record<string, unknown>) {
  const searchParams = new URLSearchParams();
  ['eventId', 'status', 'exhibitorId'].forEach((key) => {
    const value = params[key];
    if (value) searchParams.set(key, String(value));
  });
  const query = searchParams.toString();
  return query ? `?${query}` : '';
}

function formatRegistrationCategory(record: Registration) {
  if (record.categoryName && record.category) return `${record.categoryName}（${record.category}）`;
  return record.categoryName || record.category || '-';
}

export default function AdminRegistrationListPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const actionRef = useRef<ActionType>();
  const { events, eventValueEnum } = useEvents();
  const [exhibitors, setExhibitors] = useState<Exhibitor[]>([]);
  const statusHint = searchParams.get('status') ?? undefined;

  useEffect(() => {
    let alive = true;
    apiGet<Exhibitor[]>('/api/exhibitors')
      .then((loaded) => {
        if (alive) setExhibitors(loaded);
      })
      .catch(() => {
        if (alive) setExhibitors([]);
      });
    return () => { alive = false; };
  }, []);

  const exhibitorNameById = useMemo(
    () => new Map(exhibitors.map((exhibitor) => [exhibitor.id, exhibitor.companyName])),
    [exhibitors],
  );
  const exhibitorValueEnum = useMemo(
    () => buildValueEnum(exhibitors.map((exhibitor) => ({ label: exhibitor.companyName, value: exhibitor.id }))),
    [exhibitors],
  );
  const statusValueEnum = useMemo(
    () => Object.fromEntries(registrationStatuses.map((status) => [status, { text: formatRegistrationStatus(status) }])),
    [],
  );

  const columns: ProColumns<Registration>[] = [
    {
      title: '展会',
      dataIndex: 'eventId',
      valueType: 'select',
      valueEnum: eventValueEnum,
      render: (_, record) => formatEventDisplay(record.eventId, events),
    },
    {
      title: '展商',
      dataIndex: 'exhibitorId',
      valueType: 'select',
      valueEnum: exhibitorValueEnum,
      render: (_, record) => exhibitorNameById.get(record.exhibitorId) || `展商 ${formatShortId(record.exhibitorId)}`,
    },
    {
      title: '报名类别',
      dataIndex: 'category',
      hideInSearch: true,
      render: (_, record) => formatRegistrationCategory(record),
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: statusValueEnum,
      render: (_, record) => (
        <Tag color={getRegistrationStatusColor(record.status)}>{formatRegistrationStatus(record.status)}</Tag>
      ),
    },
    {
      title: '提交时间',
      dataIndex: 'submittedAt',
      valueType: 'dateTime',
      hideInSearch: true,
      render: (_, record) => (record.submittedAt ? formatFriendlyTime(record.submittedAt) : '-'),
    },
  ];

  return (
    <PageContainer title="报名审核">
      <ProTable<Registration>
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 'auto' }}
        params={{ status: statusHint }}
        form={{ initialValues: { status: statusHint } }}
        columns={columns}
        request={async (params) => {
          const data = await apiGet<Registration[]>(`/api/registrations${buildQuery(params)}`);
          return { data, success: true, total: data.length };
        }}
        onRow={(record) => ({
          onClick: () => navigate(`/admin/registrations/${record.id}`),
          style: { cursor: 'pointer' },
        })}
      />
    </PageContainer>
  );
}
