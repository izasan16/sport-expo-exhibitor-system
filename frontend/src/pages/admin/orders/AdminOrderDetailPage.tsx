import { PageContainer, ProCard } from '@ant-design/pro-components';
import { Result, Typography } from 'antd';
import { useParams } from 'react-router-dom';

export default function AdminOrderDetailPage() {
  const { orderId } = useParams();

  return (
    <PageContainer title="订单详情">
      <ProCard>
        <Result
          status="info"
          title="后台订单详情正在装配"
          subTitle={
            <Typography.Text type="secondary">
              订单 {orderId ?? '-'} 的销售确认、合同、付款和选位审核信息将在后续任务接入。
            </Typography.Text>
          }
        />
      </ProCard>
    </PageContainer>
  );
}
