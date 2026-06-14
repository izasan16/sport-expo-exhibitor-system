import { PageContainer, ProCard } from '@ant-design/pro-components';
import { Result, Typography } from 'antd';
import { useParams } from 'react-router-dom';

export default function ExhibitorOrderDetailPage() {
  const { orderId } = useParams();

  return (
    <PageContainer title="订单详情">
      <ProCard>
        <Result
          status="info"
          title="订单详情正在装配"
          subTitle={
            <Typography.Text type="secondary">
              订单 {orderId ?? '-'} 的合同、付款和选位信息将在下一步接入。
            </Typography.Text>
          }
        />
      </ProCard>
    </PageContainer>
  );
}
