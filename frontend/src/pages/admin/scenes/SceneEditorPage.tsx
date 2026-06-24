import { UploadOutlined } from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';
import { Alert, Button, Card, Col, Descriptions, Input, List, Modal, Row, Space, Tag, Typography, Upload, message } from 'antd';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { createViewportState, fitViewportToBounds, zoomViewport } from '../../../app/sceneViewport';
import {
  clearSceneBackground,
  getSceneDraft,
  getSceneRevision,
  getSceneRevisions,
  publishSceneDraft,
  saveSceneBackground,
  saveSceneDraft,
  uploadSceneFile,
  validateSceneDraft,
} from '../../../app/sceneApi';
import type { SceneDraftState, SceneFeatureType, SceneRevisionSummary, SceneValidationResult } from '../../../app/types/scene';
import SceneInspectorPanel from './SceneInspectorPanel';
import SceneLayersPanel from './SceneLayersPanel';
import SceneToolbar from './SceneToolbar';
import SceneViewportControls from './SceneViewportControls';
import { applyCompletedDrawing, canCompleteDrawing } from './sceneEditorDrawing';
import {
  applyCalibrationRatio,
  applyServerDraft,
  createEmptySceneDraft,
  removeFeature,
  toggleSelectedFeature,
  updateFeature,
} from './sceneEditorState';
import CalibrateScaleModal from './CalibrateScaleModal';
import SceneCanvas from './SceneCanvas';
import SceneRevisionDrawer from './SceneRevisionDrawer';
import { validateSceneBackgroundFile } from './sceneUploadValidation';

export default function SceneEditorPage() {
  const { eventId = '' } = useParams();
  const [draft, setDraft] = useState<SceneDraftState>(createEmptySceneDraft());
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [calibrationMode, setCalibrationMode] = useState(false);
  const [calibrationPoints, setCalibrationPoints] = useState<Array<{ x: number; y: number }>>([]);
  const [calibrateOpen, setCalibrateOpen] = useState(false);
  const [validation, setValidation] = useState<SceneValidationResult>();
  const [publishOpen, setPublishOpen] = useState(false);
  const [publishSummary, setPublishSummary] = useState('发布馆图');
  const [revisions, setRevisions] = useState<SceneRevisionSummary[]>([]);
  const [revisionDrawerOpen, setRevisionDrawerOpen] = useState(false);
  const [revisionLoading, setRevisionLoading] = useState(false);
  const [drawingFeatureType, setDrawingFeatureType] = useState<SceneFeatureType>();
  const [drawingPoints, setDrawingPoints] = useState<Array<[number, number]>>([]);
  const [viewport, setViewport] = useState(() => createViewportState());

  useEffect(() => {
    let alive = true;

    async function load() {
      try {
        const loaded = await getSceneDraft(eventId);
        if (alive) {
          setDraft((current) => applyServerDraft(current, loaded));
        }
      } catch {
        if (alive) {
          message.error('馆图草稿加载失败');
        }
      } finally {
        if (alive) {
          setLoading(false);
        }
      }
    }

    void load();
    return () => {
      alive = false;
    };
  }, [eventId]);

  const backgroundUrl = useMemo(() => draft.background?.url ?? null, [draft.background]);
  const backgroundName = useMemo(() => draft.background?.fileName ?? null, [draft.background]);
  const selectedFeature = useMemo(
    () => draft.features.find((feature) => feature.id === draft.selectedFeatureId),
    [draft.features, draft.selectedFeatureId],
  );
  const canDeleteSelected = Boolean(selectedFeature);
  const canFinishDrawing = canCompleteDrawing(drawingPoints);

  const autoFitViewport = useCallback(() => {
    if (draft.background) {
      setViewport(
        fitViewportToBounds(
          { minX: 0, minY: 0, width: draft.canvasWidth, height: draft.canvasHeight },
          { canvasWidth: draft.canvasWidth, canvasHeight: draft.canvasHeight },
        ),
      );
      return;
    }

    if (draft.features.length > 0) {
      const points = draft.features.flatMap((feature) => feature.geometry.coordinates[0] ?? []);
      if (points.length > 0) {
        const xs = points.map(([x]) => x);
        const ys = points.map(([, y]) => y);
        setViewport(
          fitViewportToBounds(
            {
              minX: Math.min(...xs),
              minY: Math.min(...ys),
              width: Math.max(...xs) - Math.min(...xs),
              height: Math.max(...ys) - Math.min(...ys),
            },
            { canvasWidth: draft.canvasWidth, canvasHeight: draft.canvasHeight },
          ),
        );
      }
    }
  }, [draft.background, draft.canvasHeight, draft.canvasWidth, draft.features]);

  useEffect(() => {
    if (loading) {
      return;
    }
    autoFitViewport();
  }, [autoFitViewport, loading]);

  const handleCalibrate = async (meters: number) => {
    const [point1, point2] = calibrationPoints;
    const pixelDistance = Math.hypot(point2.x - point1.x, point2.y - point1.y);
    const ratio = Number((pixelDistance / meters).toFixed(4));
    const next = applyCalibrationRatio(draft, ratio);
    const saved = await saveSceneDraft(eventId, {
      canvasWidth: next.canvasWidth,
      canvasHeight: next.canvasHeight,
      pixelToMeterRatio: next.pixelToMeterRatio,
      features: next.features,
    });
    setDraft((current) => applyServerDraft({ ...current, pixelToMeterRatio: ratio }, saved));
    setCalibrateOpen(false);
    setCalibrationMode(false);
    setCalibrationPoints([]);
    message.success(`比例尺已校准：${ratio.toFixed(2)} 像素/米`);
  };

  const handleUploadBackground = async (file: File) => {
    const validationResult = validateSceneBackgroundFile(file);
    if (!validationResult.valid) {
      message.error(validationResult.message!);
      return;
    }

    try {
      const uploadResult = await uploadSceneFile(file);
      const saved = await saveSceneBackground(eventId, {
        fileId: uploadResult.fileId,
        fileName: file.name,
        mimeType: file.type || uploadResult.mimeType,
        offsetX: 0,
        offsetY: 0,
        scale: 1,
        opacity: 0.45,
        locked: true,
      });
      setDraft((current) => applyServerDraft(current, saved));
      setCalibrationMode(true);
      setCalibrateOpen(true);
      setCalibrationPoints([]);
      message.success('参考底图已上传，请校准比例尺');
    } catch {
      // request helper already handles message output
    }
  };

  const startDrawing = (featureType: SceneFeatureType) => {
    setCalibrationMode(false);
    setCalibrationPoints([]);
    setDrawingFeatureType(featureType);
    setDrawingPoints([]);
    setDraft((current) => toggleSelectedFeature(current, undefined));
  };

  const cancelDrawing = () => {
    setDrawingFeatureType(undefined);
    setDrawingPoints([]);
  };

  const completeDrawing = () => {
    if (!drawingFeatureType || !canFinishDrawing) {
      return;
    }
    setDraft((current) => applyCompletedDrawing(current, drawingFeatureType, drawingPoints));
    setDrawingFeatureType(undefined);
    setDrawingPoints([]);
    message.success(drawingFeatureType === 'HALL_BOUNDARY' ? '主馆边界已更新' : '图形要素已加入草稿');
  };

  const deleteSelectedFeature = () => {
    if (!selectedFeature?.id) {
      return;
    }
    setDraft((current) => removeFeature(current, selectedFeature.id!));
    message.success('已删除选中要素');
  };

  const handleClearBackground = async () => {
    const saved = await clearSceneBackground(eventId);
    setDraft((current) => applyServerDraft(current, saved));
    setCalibrationMode(false);
    setCalibrationPoints([]);
    message.success('底图已清除');
  };

  const handleSaveDraft = async () => {
    setSaving(true);
    try {
      const saved = await saveSceneDraft(eventId, {
        canvasWidth: draft.canvasWidth,
        canvasHeight: draft.canvasHeight,
        pixelToMeterRatio: draft.pixelToMeterRatio,
        features: draft.features,
      });
      setDraft((current) => applyServerDraft(current, saved));
      message.success('草稿已保存');
    } finally {
      setSaving(false);
    }
  };

  const handleValidate = async () => {
    const result = await validateSceneDraft(eventId, {
      canvasWidth: draft.canvasWidth,
      canvasHeight: draft.canvasHeight,
      pixelToMeterRatio: draft.pixelToMeterRatio,
      features: draft.features,
    });
    setValidation(result);
    message[result.valid ? 'success' : 'warning'](result.valid ? '场景校验通过' : '场景存在待修正问题');
  };

  const handlePublish = async () => {
    setPublishing(true);
    try {
      await publishSceneDraft(eventId, publishSummary.trim() || '发布馆图');
      const latestDraft = await getSceneDraft(eventId);
      setDraft((current) => applyServerDraft(current, latestDraft));
      setPublishOpen(false);
      setValidation(undefined);
      message.success('馆图已发布，系统已生成新的草稿副本');
    } finally {
      setPublishing(false);
    }
  };

  const openRevisionDrawer = async () => {
    setRevisionLoading(true);
    setRevisionDrawerOpen(true);
    try {
      const items = await getSceneRevisions(eventId);
      setRevisions(items);
    } finally {
      setRevisionLoading(false);
    }
  };

  const previewRevision = async (revisionId: string) => {
    const revision = await getSceneRevision(eventId, revisionId);
    setDraft((current) => applyServerDraft(current, revision));
    setDrawingFeatureType(undefined);
    setDrawingPoints([]);
    setCalibrationMode(false);
    setCalibrationPoints([]);
    setRevisionDrawerOpen(false);
    message.info('已载入所选版本预览');
  };

  return (
    <PageContainer title="馆图编辑器" loading={loading}>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <SceneToolbar
          uploadTrigger={(
            <Upload
              accept=".png,.jpg,.jpeg,.svg,.pdf"
              maxCount={1}
              showUploadList={false}
              beforeUpload={async (file) => {
                await handleUploadBackground(file);
                return false;
              }}
            >
              <Button icon={<UploadOutlined />}>上传 CAD 导出底图（PNG / JPG / SVG / PDF，最大 10MB）</Button>
            </Upload>
          )}
          viewportControls={(
            <SceneViewportControls
              onFit={autoFitViewport}
              onZoomIn={() => setViewport((current) => zoomViewport(current, current.zoom * 1.2))}
              onZoomOut={() => setViewport((current) => zoomViewport(current, current.zoom / 1.2))}
              onReset={autoFitViewport}
            />
          )}
          canCompleteDrawing={canFinishDrawing}
          canDeleteSelected={canDeleteSelected}
          hasBackground={Boolean(backgroundUrl)}
          saving={saving}
          publishing={publishing}
          onUploadBackground={() => undefined}
          onDrawHall={() => startDrawing('HALL_BOUNDARY')}
          onDrawBooth={() => startDrawing('BOOTH')}
          onCompleteDrawing={completeDrawing}
          onCancelDrawing={cancelDrawing}
          onDeleteSelected={deleteSelectedFeature}
          onCalibrate={() => {
            setCalibrationMode(true);
            setCalibrateOpen(true);
            setCalibrationPoints([]);
            setDrawingFeatureType(undefined);
            setDrawingPoints([]);
          }}
          onClearBackground={() => void handleClearBackground()}
          onSaveDraft={() => void handleSaveDraft()}
          onValidate={() => void handleValidate()}
          onOpenHistory={() => void openRevisionDrawer()}
          onPublish={() => setPublishOpen(true)}
        />

        <Alert
          showIcon
          type="info"
          message="先上传 CAD 导出底图并校准比例尺，再绘制主馆边界和展位范围。发布后，场景里的展位会同步成真实 Booth 主数据。"
        />
        {validation && !validation.valid ? (
          <Alert
            showIcon
            type="warning"
            message="发布前还有待修正问题"
            description={(
              <List
                size="small"
                dataSource={validation.issues}
                renderItem={(item) => (
                  <List.Item>
                    <Space direction="vertical" size={0}>
                      <Typography.Text strong>{item.code}</Typography.Text>
                      <Typography.Text type="secondary">
                        {item.featureCode ? `${item.featureCode} · ` : ''}
                        {item.message}
                      </Typography.Text>
                    </Space>
                  </List.Item>
                )}
              />
            )}
          />
        ) : null}
        <Row gutter={16}>
          <Col xs={24} lg={5}>
            <SceneLayersPanel
              features={draft.features}
              selectedFeatureId={draft.selectedFeatureId}
              onSelectFeature={(featureId) => setDraft((current) => toggleSelectedFeature(current, featureId))}
            />
          </Col>
          <Col xs={24} lg={14}>
            <SceneCanvas
              width={draft.canvasWidth}
              height={draft.canvasHeight}
              backgroundUrl={backgroundUrl}
              calibrationEnabled={calibrationMode}
              calibrationPoints={calibrationPoints}
              features={draft.features}
              selectedFeatureId={draft.selectedFeatureId}
              drawingFeatureType={drawingFeatureType}
              drawingPoints={drawingPoints}
              viewport={viewport}
              onViewportChange={setViewport}
              onCanvasPointPick={(point) => {
                if (calibrationMode) {
                  setCalibrationPoints((current) => (current.length >= 2 ? [point] : [...current, point]));
                  return;
                }
                if (drawingFeatureType) {
                  setDrawingPoints((current) => [...current, [point.x, point.y]]);
                }
              }}
              onSelectFeature={(featureId) => setDraft((current) => toggleSelectedFeature(current, featureId))}
            />
          </Col>
          <Col xs={24} lg={5} style={{ minWidth: 320 }}>
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              <Card title="当前草稿">
                <Descriptions column={1} size="small">
                  <Descriptions.Item label="场景状态">
                    <Tag color="blue">{draft.status || 'DRAFT'}</Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="底图文件">
                    <Typography.Text ellipsis={{ tooltip: backgroundName || undefined }}>
                      {backgroundName || '未上传'}
                    </Typography.Text>
                  </Descriptions.Item>
                  <Descriptions.Item label="画布尺寸">
                    {draft.canvasWidth} x {draft.canvasHeight}
                  </Descriptions.Item>
                  <Descriptions.Item label="比例尺">
                    {draft.pixelToMeterRatio.toFixed(4)} px/m
                  </Descriptions.Item>
                  <Descriptions.Item label="当前修订">
                    <Typography.Text ellipsis={{ tooltip: draft.revisionId || undefined }}>
                      {draft.revisionId ? `${draft.revisionId.slice(0, 8)}...${draft.revisionId.slice(-4)}` : '未生成'}
                    </Typography.Text>
                  </Descriptions.Item>
                  <Descriptions.Item label="要素数量">
                    {draft.features.length}
                  </Descriptions.Item>
                </Descriptions>
                <Typography.Paragraph type="secondary" style={{ marginTop: 12, marginBottom: 0 }}>
                  当前版本支持管理员沿底图描摹轮廓，再通过右侧属性面板修正展位号和展位类型。
                </Typography.Paragraph>
              </Card>
              <SceneInspectorPanel
                draft={draft}
                selectedFeature={selectedFeature}
                onSceneChange={(patch) => setDraft((current) => ({ ...current, ...patch }))}
                onFeatureChange={(featureId, patch) => {
                  setDraft((current) => updateFeature(current, featureId, patch));
                }}
              />
            </Space>
          </Col>
        </Row>
      </Space>

      <CalibrateScaleModal
        open={calibrateOpen}
        selectedPoints={calibrationPoints}
        onCancel={() => {
          setCalibrateOpen(false);
          setCalibrationMode(false);
          setCalibrationPoints([]);
        }}
        onConfirm={(meters) => {
          void handleCalibrate(meters);
        }}
      />

      <Modal
        open={publishOpen}
        title="发布馆图"
        okText="确认发布"
        cancelText="取消"
        okButtonProps={{ loading: publishing }}
        onOk={() => void handlePublish()}
        onCancel={() => setPublishOpen(false)}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          <Typography.Text type="secondary">
            发布后销售和展商将读取当前已发布版本，同时系统会保留新的草稿副本供后续继续编辑。
          </Typography.Text>
          <Input.TextArea
            rows={4}
            value={publishSummary}
            onChange={(event) => setPublishSummary(event.target.value)}
            placeholder="填写本次发布说明"
          />
        </Space>
      </Modal>

      <SceneRevisionDrawer
        open={revisionDrawerOpen}
        revisions={revisions}
        loading={revisionLoading}
        onClose={() => setRevisionDrawerOpen(false)}
        onPreviewRevision={(revisionId) => {
          void previewRevision(revisionId);
        }}
      />
    </PageContainer>
  );
}
