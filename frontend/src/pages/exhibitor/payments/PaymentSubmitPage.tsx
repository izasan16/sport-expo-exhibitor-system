import { PageContainer, ProForm, ProFormDigit, ProFormSelect, ProFormText } from '@ant-design/pro-components';
import { message } from 'antd';
import { useParams, useSearchParams } from 'react-router-dom';
import { apiPost } from '../../../app/request';

type PaymentValues = {
  orderId: string;
  amount: number;
  method: string;
};

type PaymentResponse = {
  id: string;
  status: string;
};

export default function PaymentSubmitPage() {
  const { orderId: orderIdParam } = useParams();
  const [searchParams] = useSearchParams();
  const orderId = orderIdParam ?? searchParams.get('orderId') ?? undefined;

  return (
    <PageContainer title="提交付款">
      <ProForm<PaymentValues>
        layout="horizontal"
        initialValues={{ orderId, method: 'OFFLINE_TRANSFER' }}
        submitter={{ searchConfig: { submitText: '提交付款' } }}
        onFinish={async (values) => {
          const payment = await apiPost<PaymentResponse>('/api/payments', values);
          message.success(`付款 ${payment.id} 已提交，状态：${payment.status}`);
          return true;
        }}
      >
        <ProFormText name="orderId" label="订单 ID" rules={[{ required: true, message: '请输入订单 ID' }]} />
        <ProFormDigit
          name="amount"
          label="付款金额"
          min={0.01}
          fieldProps={{ precision: 2 }}
          rules={[{ required: true, message: '请输入付款金额' }]}
        />
        <ProFormSelect
          name="method"
          label="付款方式"
          initialValue="OFFLINE_TRANSFER"
          rules={[{ required: true, message: '请选择付款方式' }]}
          options={[{ label: 'OFFLINE_TRANSFER', value: 'OFFLINE_TRANSFER' }]}
        />
      </ProForm>
    </PageContainer>
  );
}
