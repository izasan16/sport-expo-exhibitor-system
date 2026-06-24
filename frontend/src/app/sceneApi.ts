import { apiDelete, apiGet, apiMultipartPost, apiPost, apiPut } from './request';
import type {
  SceneDraftState,
  SceneFeaturePayload,
  PublishedSceneBooth,
  PublishedSceneResponse,
  SceneRevisionSummary,
  SceneValidationResult,
  UploadedFileResponse,
} from './types/scene';

export async function uploadSceneFile(file: File): Promise<UploadedFileResponse> {
  const formData = new FormData();
  formData.append('file', file);
  return apiMultipartPost<UploadedFileResponse>('/api/files/upload', formData);
}

export async function getSceneDraft(eventId: string): Promise<SceneDraftState> {
  return apiGet<SceneDraftState>(`/api/events/${encodeURIComponent(eventId)}/scene`);
}

export async function saveSceneDraft(eventId: string, body: {
  canvasWidth: number;
  canvasHeight: number;
  pixelToMeterRatio: number;
  features: SceneFeaturePayload[];
}): Promise<SceneDraftState> {
  return apiPost<SceneDraftState>(`/api/events/${encodeURIComponent(eventId)}/scene/draft`, body);
}

export async function saveSceneBackground(eventId: string, body: {
  fileId: string;
  fileName: string;
  mimeType: string;
  offsetX: number;
  offsetY: number;
  scale: number;
  opacity: number;
  locked: boolean;
}): Promise<SceneDraftState> {
  return apiPut<SceneDraftState>(`/api/events/${encodeURIComponent(eventId)}/scene/background`, body);
}

export async function clearSceneBackground(eventId: string): Promise<SceneDraftState> {
  return apiDelete<SceneDraftState>(`/api/events/${encodeURIComponent(eventId)}/scene/background`);
}

export async function validateSceneDraft(eventId: string, body: {
  canvasWidth: number;
  canvasHeight: number;
  pixelToMeterRatio: number;
  features: SceneFeaturePayload[];
}): Promise<SceneValidationResult> {
  return apiPost<SceneValidationResult>(`/api/events/${encodeURIComponent(eventId)}/scene/draft/validate`, body);
}

export async function publishSceneDraft(eventId: string, changeSummary: string): Promise<SceneDraftState> {
  return apiPost<SceneDraftState>(`/api/events/${encodeURIComponent(eventId)}/scene/draft/publish`, { changeSummary });
}

export async function getSceneRevisions(eventId: string): Promise<SceneRevisionSummary[]> {
  return apiGet<SceneRevisionSummary[]>(`/api/events/${encodeURIComponent(eventId)}/scene/revisions`);
}

export async function getSceneRevision(eventId: string, revisionId: string): Promise<SceneDraftState> {
  return apiGet<SceneDraftState>(
    `/api/events/${encodeURIComponent(eventId)}/scene/revisions/${encodeURIComponent(revisionId)}`,
  );
}

export async function getPublishedSceneBooths(eventId: string): Promise<PublishedSceneBooth[]> {
  return apiGet<PublishedSceneBooth[]>(`/api/events/${encodeURIComponent(eventId)}/scene/published/booths`);
}

export async function getPublishedScene(eventId: string): Promise<PublishedSceneResponse> {
  return apiGet<PublishedSceneResponse>(`/api/events/${encodeURIComponent(eventId)}/scene/published`);
}
