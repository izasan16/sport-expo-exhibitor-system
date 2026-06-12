import { PageContainer, ProTable } from '@ant-design/pro-components';
import { apiGet } from '../../../app/request';

type BoothRow = {
  id: string;
  eventId: string;
  boothNo: string;
  areaSqm: number;
  boothType: string;
  status: string;
};

export default function BoothListPage() {
  return (
    <PageContainer title="展位管理">
      <ProTable
        rowKey="id"
        locale={{ emptyText: '请选择展会后查看展位' }}
        search={{ labelWidth: 'auto' }}
        columns={[
          { title: '展会 ID', dataIndex: 'eventId', hideInTable: true },
          { title: '展位号', dataIndex: 'boothNo' },
          { title: '面积', dataIndex: 'areaSqm', valueType: 'digit' },
          { title: '类型', dataIndex: 'boothType' },
          { title: '状态', dataIndex: 'status' },
        ]}
        request={async (params) => {
          if (!params.eventId) {
            return { data: [], success: true, total: 0 };
          }

          const eventId = encodeURIComponent(String(params.eventId));
          const data = await apiGet<BoothRow[]>(`/api/booths?eventId=${eventId}`);
          return { data, success: true, total: data.length };
        }}
      />
    </PageContainer>
  );
}
