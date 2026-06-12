import { PageContainer, ProCard } from '@ant-design/pro-components';
import { Col, Row, Statistic } from 'antd';

export default function DashboardPage() {
  return (
    <PageContainer title="工作台">
      <Row gutter={16}>
        <Col span={6}><ProCard><Statistic title="待审核报名" value={0} /></ProCard></Col>
        <Col span={6}><ProCard><Statistic title="待销售确认订单" value={0} /></ProCard></Col>
        <Col span={6}><ProCard><Statistic title="待审核选位" value={0} /></ProCard></Col>
        <Col span={6}><ProCard><Statistic title="待处理释放待办" value={0} /></ProCard></Col>
      </Row>
    </PageContainer>
  );
}
