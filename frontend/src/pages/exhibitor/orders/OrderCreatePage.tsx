import { PageContainer, ProForm, ProFormSelect, ProFormText } from '@ant-design/pro-components';
import { message } from 'antd';
import { apiPost } from '../../../app/request';
import { buildOrderCreateBody, type BoothType } from '../pageLogic';

type OrderCreateValues = {
  eventId: string;
  areaTierId: string;
  boothType: BoothType;
  zoneId?: string;
};

type CreatedOrder = {
  id: string;
  status: string;
  totalAmount: number | string;
};

export default function OrderCreatePage() {
  return (
    <PageContainer title="我要下单">
      <ProForm<OrderCreateValues>
        layout="horizontal"
        submitter={{ searchConfig: { submitText: '提交订单' } }}
        onFinish={async (values) => {
          const created = await apiPost<CreatedOrder>('/api/participation-orders', buildOrderCreateBody(values));
          message.success(`订单 ${created.id} 创建成功，状态：${created.status}，总金额：${created.totalAmount}`);
          return true;
        }}
      >
        <ProFormText name="eventId" label="展会 ID" rules={[{ required: true, message: '请输入展会 ID' }]} />
        <ProFormText name="areaTierId" label="面积档位 ID" rules={[{ required: true, message: '请输入面积档位 ID' }]} />
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
        <ProFormText name="zoneId" label="分区 ID" />
      </ProForm>
    </PageContainer>
  );
}
