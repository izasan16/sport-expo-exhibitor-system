import { Card, Steps } from 'antd';

export default function HomePage() {
  return (
    <Card title="参展进度">
      <Steps
        current={0}
        items={[
          { title: '报名' },
          { title: '选平米数' },
          { title: '签合同' },
          { title: '付款' },
          { title: '选位置' },
          { title: '服务办理' },
        ]}
      />
    </Card>
  );
}
