import { Button, Card, Drawer, Empty, Space, Tag, Typography } from 'antd';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { createViewportState, fitViewportToBounds, toViewBox, zoomViewport } from '../../../app/sceneViewport';
import { formatArea } from '../../../app/formatters';
import type { SceneSelectableBooth } from '../../../app/types/scene';
import { formatBoothType } from '../../../app/display';
import SceneViewportControls from '../../admin/scenes/SceneViewportControls';

type SceneSelectionMapProps = {
  booths: SceneSelectableBooth[];
  onToggle: (boothId: string, nextSelected: boolean) => void;
  readOnly?: boolean;
  mobile?: boolean;
  title?: string;
  framed?: boolean;
  backgroundUrl?: string | null;
  canvasWidth?: number;
  canvasHeight?: number;
  showViewportControls?: boolean;
};

function ringOf(booth: SceneSelectableBooth) {
  return booth.geometry.coordinates[0] ?? [];
}

function pathOf(points: Array<[number, number]>) {
  return points.map(([x, y], index) => `${index === 0 ? 'M' : 'L'} ${x} ${y}`).join(' ') + ' Z';
}

function computeBounds(booths: SceneSelectableBooth[]) {
  const points = booths.flatMap((booth) => ringOf(booth));
  if (points.length === 0) {
    return { minX: 0, minY: 0, width: 1000, height: 600 };
  }
  const xs = points.map(([x]) => x);
  const ys = points.map(([, y]) => y);
  const minX = Math.min(...xs);
  const minY = Math.min(...ys);
  const maxX = Math.max(...xs);
  const maxY = Math.max(...ys);
  return {
    minX: minX - 20,
    minY: minY - 20,
    width: Math.max(200, maxX - minX + 40),
    height: Math.max(160, maxY - minY + 40),
  };
}

function fillColor(booth: SceneSelectableBooth) {
  if (booth.highlighted) return 'rgba(250,173,20,0.55)';
  if (booth.selected) return 'rgba(22,119,255,0.55)';
  if (booth.status === 'CONFIRMED') return 'rgba(82,196,26,0.45)';
  if (booth.status === 'LOCKED') return 'rgba(250,140,22,0.45)';
  if (booth.disabled) return 'rgba(0,0,0,0.18)';
  return 'rgba(82,196,26,0.25)';
}

export default function SceneSelectionMap(props: SceneSelectionMapProps) {
  const [focusedBooth, setFocusedBooth] = useState<SceneSelectableBooth>();
  const [viewport, setViewport] = useState(() => createViewportState());
  const bounds = useMemo(() => computeBounds(props.booths), [props.booths]);
  const sceneWidth = props.canvasWidth ?? bounds.width;
  const sceneHeight = props.canvasHeight ?? bounds.height;
  const viewBox = useMemo(() => toViewBox(viewport, sceneWidth, sceneHeight), [sceneHeight, sceneWidth, viewport]);
  const panRef = useRef<{ startX: number; startY: number; originPanX: number; originPanY: number } | null>(null);

  const fitCurrentContent = useCallback(() => {
    const fitBounds = props.backgroundUrl
      ? { minX: 0, minY: 0, width: sceneWidth, height: sceneHeight }
      : bounds;
    setViewport(fitViewportToBounds(fitBounds, { canvasWidth: sceneWidth, canvasHeight: sceneHeight }));
  }, [bounds, props.backgroundUrl, sceneHeight, sceneWidth]);

  useEffect(() => {
    fitCurrentContent();
  }, [fitCurrentContent]);

  const content = props.booths.length === 0 ? (
    <Empty description="当前没有可展示的馆图展位" />
  ) : (
    <Space direction="vertical" size={12} style={{ width: '100%' }}>
      {props.showViewportControls ? (
        <SceneViewportControls
          onFit={fitCurrentContent}
          onZoomIn={() => setViewport((current) => zoomViewport(current, current.zoom * 1.2))}
          onZoomOut={() => setViewport((current) => zoomViewport(current, current.zoom / 1.2))}
          onReset={fitCurrentContent}
        />
      ) : null}
      <svg
        viewBox={`${viewBox.minX} ${viewBox.minY} ${viewBox.width} ${viewBox.height}`}
        style={{
          width: '100%',
          minHeight: 360,
          display: 'block',
          background: props.backgroundUrl ? '#fff' : '#fafafa',
          borderRadius: 8,
          cursor: viewport.isPanning ? 'grabbing' : 'grab',
        }}
        onMouseDown={(event) => {
          panRef.current = {
            startX: event.clientX,
            startY: event.clientY,
            originPanX: viewport.panX,
            originPanY: viewport.panY,
          };
          setViewport((current) => ({ ...current, isPanning: true }));
        }}
        onMouseMove={(event) => {
          if (!viewport.isPanning || !panRef.current) {
            return;
          }
          const rect = event.currentTarget.getBoundingClientRect();
          const deltaX = ((event.clientX - panRef.current.startX) / rect.width) * viewBox.width;
          const deltaY = ((event.clientY - panRef.current.startY) / rect.height) * viewBox.height;
          setViewport((current) => ({
            ...current,
            panX: panRef.current!.originPanX - deltaX,
            panY: panRef.current!.originPanY - deltaY,
            isPanning: true,
          }));
        }}
        onMouseUp={() => {
          panRef.current = null;
          setViewport((current) => ({ ...current, isPanning: false }));
        }}
        onMouseLeave={() => {
          panRef.current = null;
          setViewport((current) => ({ ...current, isPanning: false }));
        }}
      >
        {props.backgroundUrl ? (
          <image
            href={props.backgroundUrl}
            x={0}
            y={0}
            width={sceneWidth}
            height={sceneHeight}
            preserveAspectRatio="xMidYMid meet"
          />
        ) : null}
        {props.booths.map((booth) => {
          const points = ringOf(booth);
          return (
            <g key={booth.boothId}>
              <path
                d={pathOf(points)}
                fill={fillColor(booth)}
                stroke={booth.highlighted ? '#d48806' : booth.selected ? '#1677ff' : '#595959'}
                strokeWidth={booth.highlighted || booth.selected ? 3 : 1.5}
                onClick={() => {
                  if (props.readOnly) {
                    return;
                  }
                  if (props.mobile) {
                    setFocusedBooth(booth);
                    return;
                  }
                  if (booth.disabled) {
                    return;
                  }
                  props.onToggle(booth.boothId, !booth.selected);
                }}
                style={{ cursor: props.readOnly ? 'default' : 'pointer' }}
              />
              <text
                x={points.reduce((sum, [x]) => sum + x, 0) / points.length}
                y={points.reduce((sum, [, y]) => sum + y, 0) / points.length}
                textAnchor="middle"
                dominantBaseline="middle"
                fontSize={14}
                fill="#262626"
                pointerEvents="none"
              >
                {booth.boothNo}
              </text>
            </g>
          );
        })}
      </svg>
    </Space>
  );

  return (
    <>
      {props.framed === false ? content : <Card title={props.title || '馆图选位'} bodyStyle={{ padding: 12 }}>{content}</Card>}

      <Drawer
        open={Boolean(props.mobile && focusedBooth)}
        placement="bottom"
        height={280}
        title={focusedBooth?.boothNo}
        onClose={() => setFocusedBooth(undefined)}
      >
        {focusedBooth ? (
          <Space direction="vertical" style={{ width: '100%' }}>
            <Typography.Text>{formatArea(focusedBooth.areaSqm)} · {formatBoothType(focusedBooth.boothType)}</Typography.Text>
            <Tag>{focusedBooth.status}</Tag>
            {!props.readOnly ? (
              <Button
                type="primary"
                disabled={focusedBooth.disabled}
                onClick={() => {
                  props.onToggle(focusedBooth.boothId, !focusedBooth.selected);
                  setFocusedBooth(undefined);
                }}
              >
                {focusedBooth.selected ? '取消选择' : '加入已选'}
              </Button>
            ) : null}
          </Space>
        ) : null}
      </Drawer>
    </>
  );
}
