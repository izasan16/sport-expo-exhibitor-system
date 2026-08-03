import { describe, expect, it } from 'vitest';
import {
  addCalibrationPoint,
  backgroundRenderAttributes,
  buildSceneDraftPayload,
  applyCalibrationRatio,
  applyServerDraft,
  createEmptySceneDraft,
  insertFeature,
  removeFeature,
  toggleSelectedFeature,
  updateFeature,
} from './sceneEditorState';

describe('sceneEditorState', () => {
  it('opens calibration only after the second canvas point', () => {
    const first = addCalibrationPoint([], { x: 12, y: 18 });
    const second = addCalibrationPoint(first.points, { x: 42, y: 58 });

    expect(first).toEqual({ points: [{ x: 12, y: 18 }], ready: false });
    expect(second).toEqual({
      points: [{ x: 12, y: 18 }, { x: 42, y: 58 }],
      ready: true,
    });
  });

  it('builds a draft payload without changing the current background transform', () => {
    const draft = {
      ...createEmptySceneDraft(),
      background: {
        fileId: 'file-1', fileName: 'hall.svg', mimeType: 'image/svg+xml', width: 1200, height: 800,
        offsetX: 12, offsetY: -8, scale: 1.4, opacity: 0.35, locked: true,
      },
    };

    expect(buildSceneDraftPayload(draft)).toEqual({
      canvasWidth: 1200,
      canvasHeight: 800,
      pixelToMeterRatio: 20,
      features: [],
    });
    expect(backgroundRenderAttributes(draft.background!)).toEqual({
      x: 12,
      y: -8,
      width: 1680,
      height: 1120,
      opacity: 0.35,
    });
  });

  it('adds a feature and selects it', () => {
    const draft = createEmptySceneDraft();
    const next = insertFeature(draft, {
      id: 'feature-1',
      featureType: 'BOOTH',
      geometryType: 'POLYGON',
      geometry: { type: 'Polygon', coordinates: [[[0, 0], [20, 0], [20, 20], [0, 20], [0, 0]]] },
      label: 'A01',
      code: 'A01',
      layerOrder: 1,
      meta: { boothNo: 'A01' },
    });

    const selected = toggleSelectedFeature(next, 'feature-1');
    expect(selected.features).toHaveLength(1);
    expect(selected.selectedFeatureId).toBe('feature-1');
  });

  it('stores the calibrated pixel-to-meter ratio', () => {
    const draft = createEmptySceneDraft();
    const calibrated = applyCalibrationRatio(draft, 18.6667);
    expect(calibrated.pixelToMeterRatio).toBe(18.6667);
  });

  it('replaces draft state with validated server payload', () => {
    const state = createEmptySceneDraft();
    const next = applyServerDraft(state, {
      sceneId: 'scene-1',
      revisionId: 'rev-2',
      status: 'DRAFT',
      canvasWidth: 1600,
      canvasHeight: 900,
      pixelToMeterRatio: 25,
      background: undefined,
      features: [],
    });

    expect(next.sceneId).toBe('scene-1');
    expect(next.revisionId).toBe('rev-2');
    expect(next.canvasWidth).toBe(1600);
  });

  it('updates and removes selected feature payloads', () => {
    const state = insertFeature(createEmptySceneDraft(), {
      id: 'feature-1',
      featureType: 'BOOTH',
      geometryType: 'POLYGON',
      geometry: { type: 'Polygon', coordinates: [[[0, 0], [20, 0], [20, 20], [0, 20], [0, 0]]] },
      label: 'A01',
      code: 'A01',
      layerOrder: 1,
      meta: { boothNo: 'A01', boothType: 'STANDARD' },
    });

    const updated = updateFeature(state, 'feature-1', {
      label: 'A01-改',
      meta: { boothNo: 'A01', boothType: 'CUSTOM' },
    });
    const removed = removeFeature(updated, 'feature-1');

    expect(updated.features[0].label).toBe('A01-改');
    expect(updated.features[0].meta).toEqual({ boothNo: 'A01', boothType: 'CUSTOM' });
    expect(removed.features).toHaveLength(0);
  });
});
