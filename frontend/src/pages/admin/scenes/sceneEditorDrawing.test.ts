import { describe, expect, it } from 'vitest';
import { createEmptySceneDraft, insertFeature } from './sceneEditorState';
import { applyCompletedDrawing, canCompleteDrawing } from './sceneEditorDrawing';

describe('sceneEditorDrawing', () => {
  it('requires at least three points before a polygon can be completed', () => {
    expect(canCompleteDrawing([])).toBe(false);
    expect(canCompleteDrawing([[0, 0], [20, 0]])).toBe(false);
    expect(canCompleteDrawing([[0, 0], [20, 0], [20, 20]])).toBe(true);
  });

  it('replaces the existing hall boundary when a new one is completed', () => {
    const state = insertFeature(createEmptySceneDraft(), {
      id: 'hall-old',
      featureType: 'HALL_BOUNDARY',
      geometryType: 'POLYGON',
      geometry: { type: 'Polygon', coordinates: [[[0, 0], [100, 0], [100, 100], [0, 100], [0, 0]]] },
      label: '旧边界',
      code: 'HALL-OLD',
      layerOrder: 1,
      meta: null,
    });

    const next = applyCompletedDrawing(
      state,
      'HALL_BOUNDARY',
      [[10, 10], [120, 10], [120, 120], [10, 120]],
      () => 'hall-new',
    );

    expect(next.features).toHaveLength(1);
    expect(next.features[0].id).toBe('hall-new');
    expect(next.features[0].geometry.coordinates[0]).toEqual([[10, 10], [120, 10], [120, 120], [10, 120], [10, 10]]);
    expect(next.selectedFeatureId).toBe('hall-new');
  });

  it('creates booth defaults from the next booth sequence', () => {
    const state = insertFeature(createEmptySceneDraft(), {
      id: 'booth-1',
      featureType: 'BOOTH',
      geometryType: 'POLYGON',
      geometry: { type: 'Polygon', coordinates: [[[0, 0], [20, 0], [20, 20], [0, 20], [0, 0]]] },
      label: 'A01',
      code: 'A01',
      layerOrder: 1,
      meta: { boothNo: 'A01', boothType: 'STANDARD' },
    });

    const next = applyCompletedDrawing(
      state,
      'BOOTH',
      [[30, 0], [50, 0], [50, 20], [30, 20]],
      () => 'booth-2',
    );
    const created = next.features.find((feature) => feature.id === 'booth-2');

    expect(created).toMatchObject({
      label: 'A02',
      code: 'A02',
      featureType: 'BOOTH',
      meta: { boothNo: 'A02', boothType: 'STANDARD' },
    });
    expect(next.selectedFeatureId).toBe('booth-2');
  });
});
