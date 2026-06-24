import { describe, expect, it } from 'vitest';
import {
  createViewportState,
  fitViewportToBounds,
  screenToScenePoint,
  toViewBox,
  zoomViewport,
} from './sceneViewport';

describe('sceneViewport', () => {
  it('fits bounds into canvas with padding', () => {
    const viewport = fitViewportToBounds(
      { minX: 0, minY: 0, width: 1000, height: 500 },
      { canvasWidth: 1200, canvasHeight: 800 },
    );

    expect(viewport.zoom).toBeGreaterThan(1);
    expect(viewport.panX).toBeLessThanOrEqual(0);
    expect(viewport.panY).toBeLessThanOrEqual(0);
  });

  it('builds a viewBox from the viewport state', () => {
    const viewport = createViewportState({ zoom: 2, panX: 100, panY: 50 });
    const viewBox = toViewBox(viewport, 1200, 800);

    expect(viewBox).toEqual({ minX: 100, minY: 50, width: 600, height: 400 });
  });

  it('maps screen coordinates back to scene coordinates', () => {
    const viewport = createViewportState({ zoom: 2, panX: 100, panY: 50 });
    const point = screenToScenePoint(
      { x: 300, y: 200 },
      { width: 1200, height: 800 },
      viewport,
      { canvasWidth: 1200, canvasHeight: 800 },
    );

    expect(point).toEqual({ x: 250, y: 150 });
  });

  it('keeps zoom inside min and max bounds', () => {
    const viewport = createViewportState({ zoom: 1, minZoom: 0.5, maxZoom: 2 });
    expect(zoomViewport(viewport, 10).zoom).toBe(2);
    expect(zoomViewport(viewport, 0.01).zoom).toBe(0.5);
  });

  it('fits a full background-sized scene into the canvas', () => {
    const viewport = fitViewportToBounds(
      { minX: 0, minY: 0, width: 1681, height: 550 },
      { canvasWidth: 1200, canvasHeight: 800 },
    );

    const viewBox = toViewBox(viewport, 1200, 800);
    expect(viewBox.width).toBeGreaterThanOrEqual(1681);
    expect(viewBox.height).toBeGreaterThanOrEqual(550);
  });

  it('fits the full published canvas when a background exists', () => {
    const viewport = fitViewportToBounds(
      { minX: 0, minY: 0, width: 1200, height: 800 },
      { canvasWidth: 1200, canvasHeight: 800 },
    );

    const viewBox = toViewBox(viewport, 1200, 800);
    expect(viewBox.minX).toBeLessThanOrEqual(0);
    expect(viewBox.minY).toBeLessThanOrEqual(0);
  });
});
