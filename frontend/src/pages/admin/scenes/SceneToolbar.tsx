import { DeleteOutlined, EnvironmentOutlined, FolderOpenOutlined, SaveOutlined } from '@ant-design/icons';
import { Button, Card, Space } from 'antd';
import type { ReactNode } from 'react';

type SceneToolbarProps = {
  uploadTrigger?: ReactNode;
  viewportControls?: ReactNode;
  canCompleteDrawing: boolean;
  canDeleteSelected: boolean;
  hasBackground: boolean;
  saving?: boolean;
  publishing?: boolean;
  readOnly?: boolean;
  onUploadBackground: () => void;
  onDrawHall: () => void;
  onDrawBooth: () => void;
  onCompleteDrawing: () => void;
  onCancelDrawing: () => void;
  onDeleteSelected: () => void;
  onCalibrate: () => void;
  onClearBackground: () => void;
  onSaveDraft: () => void;
  onValidate: () => void;
  onOpenHistory: () => void;
  onPublish: () => void;
};

export default function SceneToolbar(props: SceneToolbarProps) {
  return (
    <Card bodyStyle={{ padding: 12 }}>
      <Space wrap size={[8, 8]}>
        {props.uploadTrigger ?? (!props.readOnly ? (
          <Button icon={<FolderOpenOutlined />} onClick={props.onUploadBackground}>
            上传底图
          </Button>
        ) : null)}
        {props.viewportControls}
        <Button disabled={props.readOnly || !props.hasBackground} onClick={props.onCalibrate}>
          校准比例尺
        </Button>
        <Button disabled={props.readOnly} type="dashed" icon={<EnvironmentOutlined />} onClick={props.onDrawHall}>
          绘制主馆边界
        </Button>
        <Button disabled={props.readOnly} type="dashed" onClick={props.onDrawBooth}>
          新增展位
        </Button>
        <Button disabled={props.readOnly || !props.canCompleteDrawing} type="primary" onClick={props.onCompleteDrawing}>
          完成绘制
        </Button>
        <Button disabled={props.readOnly || !props.canCompleteDrawing} onClick={props.onCancelDrawing}>
          取消绘制
        </Button>
        <Button danger icon={<DeleteOutlined />} disabled={props.readOnly || !props.canDeleteSelected} onClick={props.onDeleteSelected}>
          删除要素
        </Button>
        <Button disabled={props.readOnly || !props.hasBackground} onClick={props.onClearBackground}>
          清除底图
        </Button>
        <Button disabled={props.readOnly} icon={<SaveOutlined />} loading={props.saving} onClick={props.onSaveDraft}>
          保存草稿
        </Button>
        <Button disabled={props.readOnly} onClick={props.onValidate}>校验</Button>
        <Button onClick={props.onOpenHistory}>发布历史</Button>
        <Button disabled={props.readOnly} type="primary" loading={props.publishing} onClick={props.onPublish}>
          发布
        </Button>
      </Space>
    </Card>
  );
}
