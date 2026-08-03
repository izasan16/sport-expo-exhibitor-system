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
  uploadSceneCadFile,
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
  addCalibrationPoint,
  applyServerDraft,
  buildSceneDraftPayload,
  createEmptySceneDraft,
  removeFeature,
  toggleSelectedFeature,
  updateFeature,
} from './sceneEditorState';
import CalibrateScaleModal from './CalibrateScaleModal';
import SceneCanvas from './SceneCanvas';
import SceneRevisionDrawer from './SceneRevisionDrawer';
import { validateSceneBackgroundFile, validateSceneCadFile } from './sceneUploadValidation';

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
  const [revisionPreview, setRevisionPreview] = useState<SceneDraftState>();
  const [drawingFeatureType, setDrawingFeatureType] = useState<SceneFeatureType>();
  const [drawingPoints, setDrawingPoints] = useState<Array<[number, number]>>([]);
  const [viewport, setViewport] = useState(() => createViewportState());
  const [cadConversion, setCadConversion] = useState<{ status: string; message: string }>();

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

  const displayedDraft = revisionPreview ?? draft;
  const backgroundUrl = useMemo(() => displayedDraft.background?.url ?? null, [displayedDraft]);
  const backgroundName = useMemo(() => displayedDraft.background?.fileName ?? null, [displayedDraft.background]);
  const selectedFeature = useMemo(
    () => displayedDraft.features.find((feature) => feature.id === draft.selectedFeatureId),
    [displayedDraft.features, draft.selectedFeatureId],
  );
  const canDeleteSelected = Boolean(selectedFeature);
  const canFinishDrawing = canCompleteDrawing(drawingPoints);

  const autoFitViewport = useCallback(() => {
    if (displayedDraft.background) {
      setViewport(
        fitViewportToBounds(
          { minX: 0, minY: 0, width: displayedDraft.canvasWidth, height: displayedDraft.canvasHeight },
          { canvasWidth: displayedDraft.canvasWidth, canvasHeight: displayedDraft.canvasHeight },
        ),
      );
      return;
    }

    if (displayedDraft.features.length > 0) {
      const points = displayedDraft.features.flatMap((feature) => feature.geometry.coordinates[0] ?? []);
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
            { canvasWidth: displayedDraft.canvasWidth, canvasHeight: displayedDraft.canvasHeight },
          ),
        );
      }
    }
  }, [displayedDraft.background, displayedDraft.canvasHeight, displayedDraft.canvasWidth, displayedDraft.features]);

  useEffect(() => {
    if (loading) {
      return;
    }
    autoFitViewport();
  }, [autoFitViewport, loading]);

  const handleCalibrate = async (meters: number) => {
    if (revisionPreview) return;
    const [point1, point2] = calibrationPoints;
    const pixelDistance = Math.hypot(point2.x - point1.x, point2.y - point1.y);
    const ratio = Number((pixelDistance / meters).toFixed(4));
    const next = applyCalibrationRatio(draft, ratio);
    const saved = await saveSceneDraft(eventId, buildSceneDraftPayload(next));
    setDraft((current) => applyServerDraft({ ...current, pixelToMeterRatio: ratio }, saved));
    setCalibrateOpen(false);
    setCalibrationMode(false);
    setCalibrationPoints([]);
    message.success(`比例尺已校准：${ratio.toFixed(2)} 像素/米`);
  };

  const handleUploadBackground = async (file: File) => {
    if (revisionPreview) return;
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
      setCalibrateOpen(false);
      setCalibrationPoints([]);
      message.success('参考底图已上传，请校准比例尺');
    } catch {
      // request helper already handles message output
    }
  };

  const handleUploadCad = async (file: File) => {
    if (revisionPreview) return;
    const validationResult = validateSceneCadFile(file);
    if (!validationResult.valid) {
      message.error(validationResult.message!);
      return;
    }
    try {
      const response = await uploadSceneCadFile(eventId, file);
      const job = response.job;
      setCadConversion({
        status: job.jobStatus,
        message: job.jobStatus === 'FAILED'
          ? (job.errorMessage || 'CAD 转换失败')
          : `转换任务 ${job.jobStatus}`,
      });
      message[job.jobStatus === 'FAILED' ? 'error' : 'success'](
        job.jobStatus === 'FAILED' ? (job.errorMessage || 'CAD 转换失败') : 'CAD 文件已提交转换',
      );
    } catch {
      // request helper already handles message output
    }
  };

  const startDrawing = (featureType: SceneFeatureType) => {
    if (revisionPreview) return;
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
    if (revisionPreview) return;
    if (!drawingFeatureType || !canFinishDrawing) {
      return;
    }
    setDraft((current) => applyCompletedDrawing(current, drawingFeatureType, drawingPoints));
    setDrawingFeatureType(undefined);
    setDrawingPoints([]);
    message.success(drawingFeatureType === 'HALL_BOUNDARY' ? '主馆边界已更新' : '图形要素已加入草稿');
  };

  const deleteSelectedFeature = () => {
    if (revisionPreview) return;
    if (!selectedFeature?.id) {
      return;
    }
    setDraft((current) => removeFeature(current, selectedFeature.id!));
    message.success('已删除选中要素');
  };

  const handleClearBackground = async () => {
    if (revisionPreview) return;
    const saved = await clearSceneBackground(eventId);
    setDraft((current) => applyServerDraft(current, saved));
    setCalibrationMode(false);
    setCalibrationPoints([]);
    message.success('底图已清除');
  };

  const handleSaveDraft = async () => {
    if (revisionPreview) return;
    setSaving(true);
    try {
      const saved = await saveSceneDraft(eventId, buildSceneDraftPayload(draft));
      setDraft((current) => applyServerDraft(current, saved));
      message.success('草稿已保存');
    } finally {
      setSaving(false);
    }
  };

  const handleValidate = async () => {
    if (revisionPreview) return;
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
    if (revisionPreview) return;
    setPublishing(true);
    try {
      await handleSaveDraft();
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
    setRevisionPreview(revision);
    setDrawingFeatureType(undefined);
    setDrawingPoints([]);
    setCalibrationMode(false);
    setCalibrationPoints([]);
    setRevisionDrawerOpen(false);
    message.info('已打开所选版本只读预览');
  };

  return (
    <PageContainer title="馆图编辑器" loading={loading}>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <SceneToolbar
          uploadTrigger={revisionPreview ? undefined : (
            <Space wrap>
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
              <Upload
                accept=".dwg,.dxf"
                maxCount={1}
                showUploadList={false}
                beforeUpload={async (file) => {
                  await handleUploadCad(file);
                  return false;
                }}
              >
                <Button>上传 DWG / DXF 并转换</Button>
              </Upload>
            </Space>
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
          readOnly={Boolean(revisionPreview)}
          onUploadBackground={() => undefined}
          onDrawHall={() => startDrawing('HALL_BOUNDARY')}
          onDrawBooth={() => startDrawing('BOOTH')}
          onCompleteDrawing={completeDrawing}
          onCancelDrawing={cancelDrawing}
          onDeleteSelected={deleteSelectedFeature}
          onCalibrate={() => {
            if (revisionPreview) return;
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

        {revisionPreview ? (
          <Alert
            showIcon
            type="warning"
            message="历史版本只读预览"
            description="当前草稿没有被覆盖。退出预览后可继续编辑当前草稿。"
            action={<Button size="small" onClick={() => setRevisionPreview(undefined)}>退出预览</Button>}
          />
        ) : null}
        <Alert
          showIcon
          type="info"
          message="先上传 CAD 导出底图并校准比例尺，再绘制主馆边界和展位范围。发布后，场景里的展位会同步成真实 Booth 主数据。"
        />
        {cadConversion ? (
          <Alert
            showIcon
            type={cadConversion.status === 'FAILED' ? 'error' : 'info'}
            message={`CAD 转换状态：${cadConversion.status}`}
            description={cadConversion.message}
          />
        ) : null}
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
              features={displayedDraft.features}
              selectedFeatureId={draft.selectedFeatureId}
              onSelectFeature={(featureId) => {
                if (!revisionPreview) {
                  setDraft((current) => toggleSelectedFeature(current, featureId));
                }
              }}
            />
          </Col>
          <Col xs={24} lg={14}>
            <SceneCanvas
              width={displayedDraft.canvasWidth}
              height={displayedDraft.canvasHeight}
              backgroundUrl={displayedDraft.background?.url ?? null}
              background={displayedDraft.background}
              calibrationEnabled={calibrationMode}
              calibrationPoints={calibrationPoints}
              features={displayedDraft.features}
              selectedFeatureId={revisionPreview ? undefined : draft.selectedFeatureId}
              drawingFeatureType={drawingFeatureType}
              drawingPoints={drawingPoints}
              viewport={viewport}
              onViewportChange={setViewport}
              onCanvasPointPick={(point) => {
                if (calibrationMode && !revisionPreview) {
                  setCalibrationPoints((current) => {
                    const next = addCalibrationPoint(current, point);
                    if (next.ready) {
                      setCalibrateOpen(true);
                    }
                    return next.points;
                  });
                  return;
                }
                if (drawingFeatureType && !revisionPreview) {
                  setDrawingPoints((current) => [...current, [point.x, point.y]]);
                }
              }}
              onSelectFeature={(featureId) => {
                if (!revisionPreview) {
                  setDraft((current) => toggleSelectedFeature(current, featureId));
                }
              }}
            />
          </Col>
          <Col xs={24} lg={5} style={{ minWidth: 320 }}>
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              <Card title="当前草稿">
                <Descriptions column={1} size="small">
                  <Descriptions.Item label="场景状态">
                    <Tag color="blue">{displayedDraft.status || 'DRAFT'}</Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="底图文件">
                    <Typography.Text ellipsis={{ tooltip: backgroundName || undefined }}>
          {backgroundName || (revisionPreview ? '历史版本无底图' : '未上传')}
                    </Typography.Text>
                  </Descriptions.Item>
                  <Descriptions.Item label="画布尺寸">
                    {displayedDraft.canvasWidth} x {displayedDraft.canvasHeight}
                  </Descriptions.Item>
                  <Descriptions.Item label="比例尺">
                    {displayedDraft.pixelToMeterRatio.toFixed(4)} px/m
                  </Descriptions.Item>
                  <Descriptions.Item label="当前修订">
                    <Typography.Text ellipsis={{ tooltip: draft.revisionId || undefined }}>
                      {displayedDraft.revisionId ? `${displayedDraft.revisionId.slice(0, 8)}...${displayedDraft.revisionId.slice(-4)}` : '未生成'}
                    </Typography.Text>
                  </Descriptions.Item>
                  <Descriptions.Item label="要素数量">
                    {displayedDraft.features.length}
                  </Descriptions.Item>
                </Descriptions>
                <Typography.Paragraph type="secondary" style={{ marginTop: 12, marginBottom: 0 }}>
                  当前版本支持管理员沿底图描摹轮廓，再通过右侧属性面板修正展位号和展位类型。
                </Typography.Paragraph>
              </Card>
              <SceneInspectorPanel
                draft={displayedDraft}
                selectedFeature={revisionPreview ? undefined : selectedFeature}
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
