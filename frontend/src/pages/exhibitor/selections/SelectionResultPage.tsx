import { PageContainer } from '@ant-design/pro-components';
import { Button, Result } from 'antd';
import { useNavigate } from 'react-router-dom';

export default function SelectionResultPage() {
  const navigate = useNavigate();
  return (
    <PageContainer title="选位结果">
      <Result
        status="info"
        title="请从订单详情查看选位结果"
        subTitle="选位结果已经合并到订单详情页，便于同时查看合同、付款和选位状态。"
        extra={<Button type="primary" onClick={() => navigate('/exhibitor/orders')}>选择订单</Button>}
      />
    </PageContainer>
  );
}
