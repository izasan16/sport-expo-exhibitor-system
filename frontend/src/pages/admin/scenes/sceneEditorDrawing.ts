import type { SceneDraftState, SceneFeaturePayload, SceneFeatureType } from '../../../app/types/scene';

export function canCompleteDrawing(points: Array<[number, number]>) {
  return points.length >= 3;
}

function closePolygon(points: Array<[number, number]>) {
  if (points.length === 0) {
    return points;
  }
  const [firstX, firstY] = points[0];
  const [lastX, lastY] = points[points.length - 1];
  if (firstX === lastX && firstY === lastY) {
    return points;
  }
  return [...points, [firstX, firstY] as [number, number]];
}

function nextLayerOrder(features: SceneFeaturePayload[]) {
  return features.reduce((max, feature) => Math.max(max, feature.layerOrder ?? 0), 0) + 1;
}

function nextBoothSequence(features: SceneFeaturePayload[]) {
  return features.filter((feature) => feature.featureType === 'BOOTH').length + 1;
}

function boothCode(sequence: number) {
  return `A${String(sequence).padStart(2, '0')}`;
}

function buildFeature(
  features: SceneFeaturePayload[],
  featureType: SceneFeatureType,
  points: Array<[number, number]>,
  featureId: string,
): SceneFeaturePayload {
  const sequence = nextBoothSequence(features);
  const code = boothCode(sequence);
  const boothMeta = featureType === 'BOOTH'
    ? { boothNo: code, boothType: 'STANDARD' }
    : null;

  return {
    id: featureId,
    featureType,
    geometryType: 'POLYGON',
    geometry: {
      type: 'Polygon',
      coordinates: [closePolygon(points)],
    },
    label: featureType === 'HALL_BOUNDARY' ? '主馆边界' : featureType === 'BOOTH' ? code : '障碍物',
    code: featureType === 'HALL_BOUNDARY' ? 'HALL-BOUNDARY' : featureType === 'BOOTH' ? code : `OBSTACLE-${featureId.slice(0, 4).toUpperCase()}`,
    layerOrder: nextLayerOrder(features),
    meta: boothMeta,
  };
}

export function applyCompletedDrawing(
  state: SceneDraftState,
  featureType: SceneFeatureType,
  points: Array<[number, number]>,
  createId: () => string = () => crypto.randomUUID(),
): SceneDraftState {
  const nextId = createId();
  const nextFeatures = featureType === 'HALL_BOUNDARY'
    ? state.features.filter((feature) => feature.featureType !== 'HALL_BOUNDARY')
    : state.features;
  const feature = buildFeature(nextFeatures, featureType, points, nextId);

  return {
    ...state,
    features: [...nextFeatures, feature],
    selectedFeatureId: nextId,
  };
}
