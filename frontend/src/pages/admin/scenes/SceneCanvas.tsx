import { Card, Empty, Tag } from 'antd';
import { useMemo, useRef } from 'react';
import { screenToScenePoint, toViewBox, type ViewportState } from '../../../app/sceneViewport';
import type { SceneFeaturePayload } from '../../../app/types/scene';

type Point = { x: number; y: number };

type SceneCanvasProps = {
  width: number;
  height: number;
  backgroundUrl?: string | null;
  calibrationEnabled: boolean;
  calibrationPoints: Point[];
  features: SceneFeaturePayload[];
  selectedFeatureId?: string;
  drawingFeatureType?: string;
  drawingPoints: Array<[number, number]>;
  viewport: ViewportState;
  onViewportChange: (next: ViewportState) => void;
  onCanvasPointPick: (point: Point) => void;
  onSelectFeature: (featureId?: string) => void;
};

function ringToPath(ring: Array<[number, number]>) {
  return ring.map(([x, y], index) => `${index === 0 ? 'M' : 'L'} ${x} ${y}`).join(' ') + ' Z';
}

function polygonPoints(feature: SceneFeaturePayload) {
  return feature.geometry.coordinates[0] ?? [];
}

function featureStroke(feature: SceneFeaturePayload, selected: boolean) {
  if (selected) return '#1677ff';
  if (feature.featureType === 'HALL_BOUNDARY') return '#13c2c2';
  return '#fa8c16';
}

function featureFill(feature: SceneFeaturePayload, selected: boolean) {
  if (selected) return 'rgba(22,119,255,0.28)';
  if (feature.featureType === 'HALL_BOUNDARY') return 'rgba(19,194,194,0.10)';
  return 'rgba(250,140,22,0.18)';
}

export default function SceneCanvas(props: SceneCanvasProps) {
  const panRef = useRef<{ startX: number; startY: number; originPanX: number; originPanY: number } | null>(null);
  const viewBox = useMemo(
    () => toViewBox(props.viewport, props.width, props.height),
    [props.height, props.viewport, props.width],
  );

  return (
    <Card
      title="场景画布"
      extra={props.calibrationEnabled ? <Tag color="processing">校准中</Tag> : props.drawingFeatureType ? <Tag color="purple">绘制中</Tag> : null}
      bodyStyle={{ padding: 12 }}
    >
      <div
        style={{
          position: 'relative',
          minHeight: 420,
          borderRadius: 8,
          overflow: 'hidden',
          background: '#f5f5f5',
          border: '1px solid #f0f0f0',
        }}
      >
        <svg
          viewBox={`${viewBox.minX} ${viewBox.minY} ${viewBox.width} ${viewBox.height}`}
          style={{
            width: '100%',
            minHeight: 420,
            display: 'block',
            background: props.backgroundUrl ? '#fff' : '#fafafa',
            cursor: props.calibrationEnabled || props.drawingFeatureType ? 'crosshair' : props.viewport.isPanning ? 'grabbing' : 'grab',
          }}
          onMouseDown={(event) => {
            if (props.calibrationEnabled || props.drawingFeatureType) {
              return;
            }
            panRef.current = {
              startX: event.clientX,
              startY: event.clientY,
              originPanX: props.viewport.panX,
              originPanY: props.viewport.panY,
            };
            props.onViewportChange({ ...props.viewport, isPanning: true });
          }}
          onMouseMove={(event) => {
            if (!props.viewport.isPanning || !panRef.current) {
              return;
            }
            const rect = event.currentTarget.getBoundingClientRect();
            const deltaX = ((event.clientX - panRef.current.startX) / rect.width) * viewBox.width;
            const deltaY = ((event.clientY - panRef.current.startY) / rect.height) * viewBox.height;
            props.onViewportChange({
              ...props.viewport,
              panX: panRef.current.originPanX - deltaX,
              panY: panRef.current.originPanY - deltaY,
              isPanning: true,
            });
          }}
          onMouseUp={() => {
            panRef.current = null;
            props.onViewportChange({ ...props.viewport, isPanning: false });
          }}
          onMouseLeave={() => {
            panRef.current = null;
            props.onViewportChange({ ...props.viewport, isPanning: false });
          }}
          onClick={(event) => {
            if (!props.calibrationEnabled && !props.drawingFeatureType) {
              props.onSelectFeature(undefined);
              return;
            }
            const rect = event.currentTarget.getBoundingClientRect();
            props.onCanvasPointPick(
              screenToScenePoint(
                { x: event.clientX - rect.left, y: event.clientY - rect.top },
                { width: rect.width, height: rect.height },
                props.viewport,
                { canvasWidth: props.width, canvasHeight: props.height },
              ),
            );
          }}
        >
          {props.backgroundUrl ? (
            <image href={props.backgroundUrl} x={0} y={0} width={props.width} height={props.height} preserveAspectRatio="xMidYMid meet" />
          ) : null}

          {props.features.map((feature) => {
            const ring = polygonPoints(feature);
            if (ring.length < 4) return null;
            const selected = props.selectedFeatureId === feature.id;
            return (
              <path
                key={feature.id}
                d={ringToPath(ring)}
                fill={featureFill(feature, selected)}
                stroke={featureStroke(feature, selected)}
                strokeWidth={selected ? 3 : 2}
                onClick={(event) => {
                  if (props.calibrationEnabled || props.drawingFeatureType) {
                    return;
                  }
                  event.stopPropagation();
                  props.onSelectFeature(feature.id);
                }}
              />
            );
          })}

          {props.drawingPoints.length > 0 ? (
            <>
              <polyline
                points={props.drawingPoints.map(([x, y]) => `${x},${y}`).join(' ')}
                fill="rgba(22,119,255,0.10)"
                stroke="#1677ff"
                strokeWidth={2}
              />
              {props.drawingPoints.map(([x, y], index) => (
                <circle key={`${x}-${y}-${index}`} cx={x} cy={y} r={6} fill="#1677ff" stroke="#fff" strokeWidth={2} />
              ))}
            </>
          ) : null}

          {props.calibrationPoints.map((point, index) => (
            <circle
              key={`${point.x}-${point.y}-${index}`}
              cx={point.x}
              cy={point.y}
              r={8}
              fill="#1677ff"
              stroke="#fff"
              strokeWidth={3}
            />
          ))}
        </svg>

        {!props.backgroundUrl && props.features.length === 0 ? (
          <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', pointerEvents: 'none' }}>
            <Empty description="先上传 CAD 导出底图，或直接开始绘制主馆边界" />
          </div>
        ) : null}
      </div>
    </Card>
  );
}
