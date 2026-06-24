export type SceneFeatureType = 'HALL_BOUNDARY' | 'BOOTH' | 'OBSTACLE' | 'TEXT' | 'IMAGE';

export type SceneGeometry = {
  type: 'Polygon';
  coordinates: Array<Array<[number, number]>>;
};

export type SceneFeaturePayload = {
  id?: string;
  featureType: SceneFeatureType;
  geometryType: string;
  geometry: SceneGeometry;
  label?: string | null;
  code?: string | null;
  layerOrder?: number | null;
  meta?: Record<string, unknown> | null;
};

export type SceneBackground = {
  fileId: string;
  fileName: string;
  mimeType: string;
  url?: string | null;
  width?: number | null;
  height?: number | null;
  offsetX: number;
  offsetY: number;
  scale: number;
  opacity: number;
  locked: boolean;
};

export type SceneDraftState = {
  sceneId?: string;
  revisionId?: string;
  status?: string;
  canvasWidth: number;
  canvasHeight: number;
  pixelToMeterRatio: number;
  background?: SceneBackground;
  features: SceneFeaturePayload[];
  selectedFeatureId?: string;
};

export type SceneRevisionSummary = {
  revisionId: string;
  revisionNo: number;
  status: string;
  changeSummary?: string | null;
  publishedAt?: string | null;
  publishedBy?: string | null;
  createdAt?: string | null;
};

export type SceneValidationIssue = {
  featureCode?: string | null;
  code: string;
  message: string;
};

export type SceneValidationResult = {
  valid: boolean;
  issues: SceneValidationIssue[];
};

export type PublishedSceneBooth = {
  boothId: string;
  boothNo: string;
  boothType: string;
  status: string;
  areaSqm: string | number;
  geometry: SceneGeometry;
};

export type PublishedSceneResponse = {
  sceneId: string;
  revisionId: string;
  status: string;
  canvasWidth: number;
  canvasHeight: number;
  pixelToMeterRatio: number;
  background?: SceneBackground;
  features: SceneFeaturePayload[];
};

export type SceneSelectableBooth = PublishedSceneBooth & {
  selected: boolean;
  disabled: boolean;
  highlighted?: boolean;
};

export type UploadedFileResponse = {
  fileId: string;
  originalName: string;
  mimeType: string;
  url: string;
  fileSize: number;
  width?: number | null;
  height?: number | null;
};
