import { Alert, Button, Form, InputNumber, Modal, Space, Typography } from 'antd';

type Point = { x: number; y: number };

type CalibrateScaleModalProps = {
  open: boolean;
  onCancel: () => void;
  onConfirm: (meters: number) => void;
  selectedPoints: Point[];
};

export default function CalibrateScaleModal(props: CalibrateScaleModalProps) {
  const pixelDistance = props.selectedPoints.length === 2
    ? Math.hypot(
      props.selectedPoints[1].x - props.selectedPoints[0].x,
      props.selectedPoints[1].y - props.selectedPoints[0].y,
    )
    : 0;

  return (
    <Modal open={props.open} title="校准比例尺" onCancel={props.onCancel} footer={null} destroyOnClose>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Alert
          showIcon
          type="info"
          message="请先在底图上点击两个已知距离的点，再输入两点间的真实距离。"
        />
        <Typography.Text type="secondary">
          已选点位：{props.selectedPoints.length} / 2
          {pixelDistance > 0 ? `，像素距离 ${pixelDistance.toFixed(2)}` : ''}
        </Typography.Text>
        <Form layout="vertical" onFinish={(values) => props.onConfirm(values.distance)}>
          <Form.Item
            label="实际距离（米）"
            name="distance"
            rules={[{ required: true, message: '请输入真实距离' }]}
          >
            <InputNumber min={0.1} precision={2} style={{ width: '100%' }} />
          </Form.Item>
          <Button type="primary" htmlType="submit" block disabled={props.selectedPoints.length < 2}>
            完成校准
          </Button>
        </Form>
      </Space>
    </Modal>
  );
}
