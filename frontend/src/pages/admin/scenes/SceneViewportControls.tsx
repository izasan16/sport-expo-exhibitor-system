import { FullscreenOutlined, MinusOutlined, PlusOutlined, RedoOutlined } from '@ant-design/icons';
import { Button, Space } from 'antd';

type SceneViewportControlsProps = {
  onFit: () => void;
  onZoomIn: () => void;
  onZoomOut: () => void;
  onReset: () => void;
};

export default function SceneViewportControls(props: SceneViewportControlsProps) {
  return (
    <Space size={8}>
      <Button icon={<FullscreenOutlined />} onClick={props.onFit}>适配画布</Button>
      <Button icon={<PlusOutlined />} onClick={props.onZoomIn}>放大</Button>
      <Button icon={<MinusOutlined />} onClick={props.onZoomOut}>缩小</Button>
      <Button icon={<RedoOutlined />} onClick={props.onReset}>重置视图</Button>
    </Space>
  );
}
