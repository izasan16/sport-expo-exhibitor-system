import { PageContainer, ProTable } from '@ant-design/pro-components';
import { apiGet } from '../../../app/request';

type EventRow = {
  id: string;
  name: string;
  year: number;
  status: string;
};

export default function EventListPage() {
  return (
    <PageContainer title="展会管理">
      <ProTable
        rowKey="id"
        search={false}
        columns={[
          { title: '展会名称', dataIndex: 'name' },
          { title: '年份', dataIndex: 'year' },
          { title: '状态', dataIndex: 'status' },
        ]}
        request={async () => {
          const data = await apiGet<EventRow[]>('/api/events');
          return { data, success: true, total: data.length };
        }}
      />
    </PageContainer>
  );
}
