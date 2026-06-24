export type ViewportState = {
  zoom: number;
  panX: number;
  panY: number;
  isPanning: boolean;
  minZoom: number;
  maxZoom: number;
};

export type SceneBounds = {
  minX: number;
  minY: number;
  width: number;
  height: number;
};

export function createViewportState(patch?: Partial<ViewportState>): ViewportState {
  return {
    zoom: 1,
    panX: 0,
    panY: 0,
    isPanning: false,
    minZoom: 0.25,
    maxZoom: 4,
    ...patch,
  };
}

export function fitViewportToBounds(
  bounds: SceneBounds,
  canvas: { canvasWidth: number; canvasHeight: number },
  padding = 24,
): ViewportState {
  const safeWidth = Math.max(1, bounds.width + padding * 2);
  const safeHeight = Math.max(1, bounds.height + padding * 2);
  const zoom = Math.min(canvas.canvasWidth / safeWidth, canvas.canvasHeight / safeHeight);
  const viewWidth = canvas.canvasWidth / zoom;
  const viewHeight = canvas.canvasHeight / zoom;

  return createViewportState({
    zoom,
    panX: bounds.minX - (viewWidth - bounds.width) / 2,
    panY: bounds.minY - (viewHeight - bounds.height) / 2,
  });
}

export function toViewBox(viewport: ViewportState, canvasWidth: number, canvasHeight: number) {
  return {
    minX: viewport.panX,
    minY: viewport.panY,
    width: canvasWidth / viewport.zoom,
    height: canvasHeight / viewport.zoom,
  };
}

export function zoomViewport(viewport: ViewportState, nextZoom: number): ViewportState {
  const zoom = Math.max(viewport.minZoom, Math.min(viewport.maxZoom, nextZoom));
  return { ...viewport, zoom };
}

export function screenToScenePoint(
  point: { x: number; y: number },
  rect: { width: number; height: number },
  viewport: ViewportState,
  canvas: { canvasWidth: number; canvasHeight: number },
) {
  const viewBox = toViewBox(viewport, canvas.canvasWidth, canvas.canvasHeight);
  return {
    x: Number((viewBox.minX + (point.x / rect.width) * viewBox.width).toFixed(2)),
    y: Number((viewBox.minY + (point.y / rect.height) * viewBox.height).toFixed(2)),
  };
}
