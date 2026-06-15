import { LeftOutlined } from '@ant-design/icons';
import { Button, Card, Space, Typography } from 'antd';

type MobileOrderContextMetric = {
  label: string;
  value: string;
};

type MobileOrderContextHeaderProps = {
  title: string;
  subtitle: string;
  backText: string;
  metrics: MobileOrderContextMetric[];
  onBack: () => void;
};

export default function MobileOrderContextHeader({
  title,
  subtitle,
  backText,
  metrics,
  onBack,
}: MobileOrderContextHeaderProps) {
  return (
    <Card className="exhibitor-context-header">
      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        <Button type="text" icon={<LeftOutlined />} onClick={onBack} className="exhibitor-context-back">
          {backText}
        </Button>
        <Space direction="vertical" size={4}>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {title}
          </Typography.Title>
          <Typography.Text type="secondary">{subtitle}</Typography.Text>
        </Space>
        <div className="exhibitor-context-metrics">
          {metrics.map((metric) => (
            <div key={metric.label} className="exhibitor-context-metric">
              <Typography.Text type="secondary">{metric.label}</Typography.Text>
              <Typography.Text strong>{metric.value}</Typography.Text>
            </div>
          ))}
        </div>
      </Space>
    </Card>
  );
}
