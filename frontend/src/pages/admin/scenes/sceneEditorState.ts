import type { SceneDraftState, SceneFeaturePayload } from '../../../app/types/scene';

export function createEmptySceneDraft(): SceneDraftState {
  return {
    canvasWidth: 1200,
    canvasHeight: 800,
    pixelToMeterRatio: 20,
    features: [],
  };
}

export function insertFeature(state: SceneDraftState, feature: SceneFeaturePayload): SceneDraftState {
  return { ...state, features: [...state.features, feature] };
}

export function toggleSelectedFeature(state: SceneDraftState, featureId?: string): SceneDraftState {
  return { ...state, selectedFeatureId: featureId };
}

export function applyCalibrationRatio(state: SceneDraftState, pixelToMeterRatio: number): SceneDraftState {
  return { ...state, pixelToMeterRatio };
}

export function applyServerDraft(state: SceneDraftState, payload: SceneDraftState): SceneDraftState {
  return {
    ...state,
    sceneId: payload.sceneId,
    revisionId: payload.revisionId,
    status: payload.status,
    canvasWidth: payload.canvasWidth,
    canvasHeight: payload.canvasHeight,
    pixelToMeterRatio: payload.pixelToMeterRatio,
    background: payload.background,
    features: payload.features,
  };
}

export function updateFeature(
  state: SceneDraftState,
  featureId: string,
  patch: Partial<SceneFeaturePayload>,
): SceneDraftState {
  return {
    ...state,
    features: state.features.map((feature) => (
      feature.id === featureId
        ? { ...feature, ...patch }
        : feature
    )),
  };
}

export function removeFeature(state: SceneDraftState, featureId: string): SceneDraftState {
  return {
    ...state,
    features: state.features.filter((feature) => feature.id !== featureId),
    selectedFeatureId: state.selectedFeatureId === featureId ? undefined : state.selectedFeatureId,
  };
}
