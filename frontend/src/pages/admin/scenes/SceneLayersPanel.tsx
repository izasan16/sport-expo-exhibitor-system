import { Card, List, Tag, Typography } from 'antd';
import type { SceneFeaturePayload } from '../../../app/types/scene';

type SceneLayersPanelProps = {
  features: SceneFeaturePayload[];
  selectedFeatureId?: string;
  onSelectFeature: (featureId?: string) => void;
};

function typeLabel(featureType: string) {
  if (featureType === 'HALL_BOUNDARY') return '主馆边界';
  if (featureType === 'BOOTH') return '展位';
  if (featureType === 'OBSTACLE') return '障碍物';
  return featureType;
}

export default function SceneLayersPanel(props: SceneLayersPanelProps) {
  return (
    <Card title="图层" bodyStyle={{ padding: 0 }}>
      <List
        size="small"
        dataSource={[...props.features].sort((a, b) => (a.layerOrder ?? 0) - (b.layerOrder ?? 0))}
        locale={{ emptyText: '还没有图形要素' }}
        renderItem={(item) => (
          <List.Item
            onClick={() => props.onSelectFeature(item.id)}
            style={{
              cursor: 'pointer',
              paddingInline: 12,
              background: props.selectedFeatureId === item.id ? '#e6f4ff' : undefined,
            }}
          >
            <List.Item.Meta
              title={(
                <Typography.Text strong={props.selectedFeatureId === item.id}>
                  {item.label || item.code || '未命名要素'}
                </Typography.Text>
              )}
              description={<Tag>{typeLabel(item.featureType)}</Tag>}
            />
          </List.Item>
        )}
      />
    </Card>
  );
}
