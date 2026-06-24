import { Card, Empty, Form, Input, InputNumber, Select, Space, Typography } from 'antd';
import { polygonAreaSqm } from '../../../app/sceneGeometry';
import type { SceneDraftState, SceneFeaturePayload } from '../../../app/types/scene';

type SceneInspectorPanelProps = {
  draft: SceneDraftState;
  selectedFeature?: SceneFeaturePayload;
  onSceneChange: (patch: Partial<Pick<SceneDraftState, 'canvasWidth' | 'canvasHeight' | 'pixelToMeterRatio'>>) => void;
  onFeatureChange: (featureId: string, patch: Partial<SceneFeaturePayload>) => void;
};

function pointsOf(feature: SceneFeaturePayload) {
  return feature.geometry.coordinates[0] ?? [];
}

export default function SceneInspectorPanel(props: SceneInspectorPanelProps) {
  if (!props.selectedFeature) {
    return (
      <Card title="属性">
        <Space direction="vertical" style={{ width: '100%' }}>
          <Typography.Text type="secondary">先选中一个要素，或先从左侧开始绘制。</Typography.Text>
          <Form layout="vertical" style={{ width: '100%' }}>
            <Form.Item label="画布宽度">
              <InputNumber
                min={400}
                value={props.draft.canvasWidth}
                style={{ width: '100%' }}
                onChange={(value) => props.onSceneChange({ canvasWidth: Number(value || 1200) })}
              />
            </Form.Item>
            <Form.Item label="画布高度">
              <InputNumber
                min={300}
                value={props.draft.canvasHeight}
                style={{ width: '100%' }}
                onChange={(value) => props.onSceneChange({ canvasHeight: Number(value || 800) })}
              />
            </Form.Item>
            <Form.Item label="像素/米">
              <InputNumber
                min={0.0001}
                precision={4}
                value={props.draft.pixelToMeterRatio}
                style={{ width: '100%' }}
                onChange={(value) => props.onSceneChange({ pixelToMeterRatio: Number(value || 20) })}
              />
            </Form.Item>
          </Form>
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="未选中要素" />
        </Space>
      </Card>
    );
  }

  const selectedFeature = props.selectedFeature;
  const featureId = selectedFeature.id;
  const meta = selectedFeature.meta ?? {};
  const area = polygonAreaSqm(pointsOf(selectedFeature), props.draft.pixelToMeterRatio);

  return (
    <Card title="属性">
      <Form layout="vertical">
        <Form.Item label="显示名称">
          <Input
            value={selectedFeature.label ?? ''}
            onChange={(event) => featureId && props.onFeatureChange(featureId, { label: event.target.value })}
          />
        </Form.Item>
        <Form.Item label="编码">
          <Input
            value={selectedFeature.code ?? ''}
            onChange={(event) => featureId && props.onFeatureChange(featureId, { code: event.target.value })}
          />
        </Form.Item>
        {selectedFeature.featureType === 'BOOTH' ? (
          <>
            <Form.Item label="展位号">
              <Input
                value={String(meta.boothNo ?? '')}
                onChange={(event) => featureId && props.onFeatureChange(featureId, {
                  meta: { ...meta, boothNo: event.target.value },
                })}
              />
            </Form.Item>
            <Form.Item label="展位类型">
              <Select
                value={String(meta.boothType ?? 'STANDARD')}
                options={[
                  { label: '标准展位', value: 'STANDARD' },
                  { label: '特装展位', value: 'CUSTOM' },
                ]}
                onChange={(value) => featureId && props.onFeatureChange(featureId, {
                  meta: { ...meta, boothType: value },
                })}
              />
            </Form.Item>
          </>
        ) : null}
        <Form.Item label="面积估算">
          <Typography.Text>{area.toFixed(2)} ㎡</Typography.Text>
        </Form.Item>
      </Form>
    </Card>
  );
}
