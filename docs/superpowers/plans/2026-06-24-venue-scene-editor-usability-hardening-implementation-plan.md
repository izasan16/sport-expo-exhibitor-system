# Venue Scene Editor Usability Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make real venue floor-plan backgrounds practical to upload, inspect, pan, zoom, and review in the existing scene editor and admin review flow.

**Architecture:** Keep the current Spring Boot + lightweight SVG scene stack. Raise the backend multipart limit to 10MB, add a small frontend upload-validation helper, and introduce a shared viewBox-based viewport utility that both the admin scene editor and the admin review preview can reuse. Avoid CSS transforms so background images, polygon features, and click-coordinate math stay in one scene coordinate system.

**Tech Stack:** Spring Boot 3, Java 17, React 18, TypeScript, Vite, Vitest, Ant Design, ProComponents, SVG `viewBox`.

---

## File Structure

Backend files to modify:

- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/sportexpo/exhibitor/file/FileUploadControllerTest.java`

Frontend files to create:

- `frontend/src/app/sceneViewport.ts`
- `frontend/src/app/sceneViewport.test.ts`
- `frontend/src/pages/admin/scenes/sceneUploadValidation.ts`
- `frontend/src/pages/admin/scenes/sceneUploadValidation.test.ts`
- `frontend/src/pages/admin/scenes/SceneViewportControls.tsx`

Frontend files to modify:

- `frontend/src/app/sceneApi.ts`
- `frontend/src/app/types/scene.ts`
- `frontend/src/pages/admin/scenes/SceneEditorPage.tsx`
- `frontend/src/pages/admin/scenes/SceneCanvas.tsx`
- `frontend/src/pages/admin/scenes/SceneToolbar.tsx`
- `frontend/src/pages/admin/scenes/SceneInspectorPanel.tsx`
- `frontend/src/pages/admin/selections/SelectionScenePreview.tsx`
- `frontend/src/pages/exhibitor/selections/SceneSelectionMap.tsx`

Verification artifacts:

- `output/playwright/admin-scene-editor-with-real-floorplan.png`
- `output/playwright/admin-selection-review-preview.png`

---

### Task 1: Backend Multipart Limit And Regression Coverage

**Files:**
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/test/java/com/sportexpo/exhibitor/file/FileUploadControllerTest.java`

- [ ] **Step 1: Add a failing oversized-upload regression test**

Update `backend/src/test/java/com/sportexpo/exhibitor/file/FileUploadControllerTest.java` with a second test and a tight multipart limit for the test class:

```java
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.file-storage.root-path=target/test-uploads",
        "spring.servlet.multipart.max-file-size=1KB",
        "spring.servlet.multipart.max-request-size=1KB"
})
class FileUploadControllerTest extends TestDatabaseSupport {
    // existing fields...

    @Test
    void rejectsFileLargerThanConfiguredMultipartLimit() throws Exception {
        byte[] oversized = new byte[2048];

        mockMvc.perform(multipart("/api/files/upload")
                        .file("file", oversized)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isPayloadTooLarge());
    }
}
```

- [ ] **Step 2: Run the backend upload test and confirm the new case fails**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn "-Dtest=FileUploadControllerTest" test
```

Expected: the new oversized case fails because the application does not yet declare multipart limits in `application.yml`.

- [ ] **Step 3: Raise the real multipart limit to 10MB**

Add this block to `backend/src/main/resources/application.yml` under `spring`:

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

- [ ] **Step 4: Keep the happy-path upload test intact**

Ensure the existing success test still looks like this in `FileUploadControllerTest.java`:

```java
@Test
void adminCanUploadSceneBackground() throws Exception {
    mockMvc.perform(multipart("/api/files/upload")
                    .file("file", "fake-png".getBytes())
                    .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fileId").isNotEmpty())
            .andExpect(jsonPath("$.data.url").isNotEmpty())
            .andExpect(jsonPath("$.data.originalName").value("file"));
}
```

- [ ] **Step 5: Re-run the backend upload test class**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn "-Dtest=FileUploadControllerTest" test
```

Expected: PASS. The class-local 1KB override should still produce `413`, and the success case should still upload a small file.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/application.yml backend/src/test/java/com/sportexpo/exhibitor/file/FileUploadControllerTest.java
git commit -m "feat: raise scene background upload limit"
```

---

### Task 2: Frontend Upload Validation Helper

**Files:**
- Create: `frontend/src/pages/admin/scenes/sceneUploadValidation.ts`
- Create: `frontend/src/pages/admin/scenes/sceneUploadValidation.test.ts`
- Modify: `frontend/src/pages/admin/scenes/SceneEditorPage.tsx`

- [ ] **Step 1: Write the failing upload-validation unit test**

Create `frontend/src/pages/admin/scenes/sceneUploadValidation.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { MAX_SCENE_BACKGROUND_BYTES, validateSceneBackgroundFile } from './sceneUploadValidation';

function createFile(size: number, name = 'floorplan.png', type = 'image/png') {
  return new File([new Uint8Array(size)], name, { type });
}

describe('sceneUploadValidation', () => {
  it('accepts files at or below 10MB', () => {
    const result = validateSceneBackgroundFile(createFile(MAX_SCENE_BACKGROUND_BYTES));
    expect(result.valid).toBe(true);
  });

  it('rejects files larger than 10MB', () => {
    const result = validateSceneBackgroundFile(createFile(MAX_SCENE_BACKGROUND_BYTES + 1));
    expect(result.valid).toBe(false);
    expect(result.message).toBe('底图文件不能超过 10MB，请压缩后重试');
  });
});
```

- [ ] **Step 2: Run the test and verify it fails**

Run:

```powershell
cd frontend
npm test -- sceneUploadValidation.test.ts
```

Expected: FAIL because the validation helper does not exist.

- [ ] **Step 3: Implement the upload-validation helper**

Create `frontend/src/pages/admin/scenes/sceneUploadValidation.ts`:

```ts
export const MAX_SCENE_BACKGROUND_BYTES = 10 * 1024 * 1024;

export function validateSceneBackgroundFile(file: File): { valid: boolean; message?: string } {
  if (file.size > MAX_SCENE_BACKGROUND_BYTES) {
    return {
      valid: false,
      message: '底图文件不能超过 10MB，请压缩后重试',
    };
  }

  return { valid: true };
}
```

- [ ] **Step 4: Use the helper before any upload request starts**

In `frontend/src/pages/admin/scenes/SceneEditorPage.tsx`, import the helper and update the upload hook:

```ts
import { validateSceneBackgroundFile } from './sceneUploadValidation';
```

```tsx
const handleUploadBackground = async (file: File) => {
  const validationResult = validateSceneBackgroundFile(file);
  if (!validationResult.valid) {
    message.error(validationResult.message!);
    return;
  }

  try {
    const uploadResult = await uploadSceneFile(file);
    // existing saveSceneBackground flow...
  } catch {
    message.error('底图上传失败，请检查文件格式或大小限制');
  }
};
```

Also update the upload trigger copy:

```tsx
<Button icon={<UploadOutlined />}>上传 CAD 导出底图（PNG / JPG / SVG / PDF，最大 10MB）</Button>
```

- [ ] **Step 5: Run the frontend validation test**

Run:

```powershell
cd frontend
npm test -- sceneUploadValidation.test.ts
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/admin/scenes/sceneUploadValidation.ts frontend/src/pages/admin/scenes/sceneUploadValidation.test.ts frontend/src/pages/admin/scenes/SceneEditorPage.tsx
git commit -m "feat: validate scene background upload size"
```

---

### Task 3: Shared Viewport Math Utility

**Files:**
- Create: `frontend/src/app/sceneViewport.ts`
- Create: `frontend/src/app/sceneViewport.test.ts`

- [ ] **Step 1: Write the failing viewport utility tests**

Create `frontend/src/app/sceneViewport.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import {
  createViewportState,
  fitViewportToBounds,
  toViewBox,
  zoomViewport,
  screenToScenePoint,
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
});
```

- [ ] **Step 2: Run the viewport tests and verify they fail**

Run:

```powershell
cd frontend
npm test -- sceneViewport.test.ts
```

Expected: FAIL because the viewport utility module does not exist.

- [ ] **Step 3: Implement the shared viewport utility**

Create `frontend/src/app/sceneViewport.ts`:

```ts
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
```

- [ ] **Step 4: Re-run the viewport tests**

Run:

```powershell
cd frontend
npm test -- sceneViewport.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/sceneViewport.ts frontend/src/app/sceneViewport.test.ts
git commit -m "feat: add shared scene viewport math"
```

---

### Task 4: Admin Scene Editor Viewport, Toolbar, And Stable Sidebar

**Files:**
- Create: `frontend/src/pages/admin/scenes/SceneViewportControls.tsx`
- Modify: `frontend/src/pages/admin/scenes/SceneToolbar.tsx`
- Modify: `frontend/src/pages/admin/scenes/SceneCanvas.tsx`
- Modify: `frontend/src/pages/admin/scenes/SceneEditorPage.tsx`
- Modify: `frontend/src/pages/admin/scenes/SceneInspectorPanel.tsx`

- [ ] **Step 1: Write a failing editor viewport test**

Extend `frontend/src/app/sceneViewport.test.ts` with a content-fit case for a background-sized scene:

```ts
it('fits a full background-sized scene into the canvas', () => {
  const viewport = fitViewportToBounds(
    { minX: 0, minY: 0, width: 1681, height: 550 },
    { canvasWidth: 1200, canvasHeight: 800 },
  );

  const viewBox = toViewBox(viewport, 1200, 800);
  expect(viewBox.width).toBeGreaterThanOrEqual(1681);
  expect(viewBox.height).toBeGreaterThanOrEqual(550);
});
```

- [ ] **Step 2: Run the viewport tests to verify the new case fails if fit logic is incomplete**

Run:

```powershell
cd frontend
npm test -- sceneViewport.test.ts
```

Expected: if your initial fit helper is incomplete, this new test should fail until the fit math handles wide backgrounds correctly.

- [ ] **Step 3: Add reusable viewport control buttons**

Create `frontend/src/pages/admin/scenes/SceneViewportControls.tsx`:

```tsx
import { FullscreenOutlined, PlusOutlined, RedoOutlined, MinusOutlined } from '@ant-design/icons';
import { Button, Space } from 'antd';

type SceneViewportControlsProps = {
  onFit: () => void;
  onZoomIn: () => void;
  onZoomOut: () => void;
  onReset: () => void;
};

export default function SceneViewportControls(props: SceneViewportControlsProps) {
  return (
    <Space size={8}>
      <Button icon={<FullscreenOutlined />} onClick={props.onFit}>适配画布</Button>
      <Button icon={<PlusOutlined />} onClick={props.onZoomIn}>放大</Button>
      <Button icon={<MinusOutlined />} onClick={props.onZoomOut}>缩小</Button>
      <Button icon={<RedoOutlined />} onClick={props.onReset}>重置视图</Button>
    </Space>
  );
}
```

- [ ] **Step 4: Extend the scene toolbar props and render the viewport controls**

Update `frontend/src/pages/admin/scenes/SceneToolbar.tsx`:

```tsx
type SceneToolbarProps = {
  // existing props...
  viewportControls?: ReactNode;
};
```

And render them inline:

```tsx
<Space wrap size={[8, 8]}>
  {props.uploadTrigger}
  {props.viewportControls}
  <Button disabled={!props.hasBackground} onClick={props.onCalibrate}>校准比例尺</Button>
  {/* existing buttons... */}
</Space>
```

- [ ] **Step 5: Add viewBox-based panning and coordinate conversion to `SceneCanvas`**

In `frontend/src/pages/admin/scenes/SceneCanvas.tsx`, add props for the viewport and replace the fixed `viewBox` usage:

```tsx
type SceneCanvasProps = {
  // existing props...
  viewport: ViewportState;
  onViewportChange: (next: ViewportState) => void;
};
```

Use the shared utility:

```tsx
const viewBox = toViewBox(props.viewport, props.width, props.height);
```

```tsx
<svg
  viewBox={`${viewBox.minX} ${viewBox.minY} ${viewBox.width} ${viewBox.height}`}
  onMouseDown={(event) => {
    if (props.calibrationEnabled || props.drawingFeatureType) return;
    panRef.current = {
      startX: event.clientX,
      startY: event.clientY,
      originPanX: props.viewport.panX,
      originPanY: props.viewport.panY,
    };
    props.onViewportChange({ ...props.viewport, isPanning: true });
  }}
  onMouseMove={(event) => {
    if (!props.viewport.isPanning || !panRef.current) return;
    const rect = event.currentTarget.getBoundingClientRect();
    const viewWidth = viewBox.width;
    const viewHeight = viewBox.height;
    const deltaX = ((event.clientX - panRef.current.startX) / rect.width) * viewWidth;
    const deltaY = ((event.clientY - panRef.current.startY) / rect.height) * viewHeight;
    props.onViewportChange({
      ...props.viewport,
      panX: panRef.current.originPanX - deltaX,
      panY: panRef.current.originPanY - deltaY,
      isPanning: true,
    });
  }}
  onMouseUp={() => props.onViewportChange({ ...props.viewport, isPanning: false })}
  onMouseLeave={() => props.onViewportChange({ ...props.viewport, isPanning: false })}
  onClick={(event) => {
    if (!props.calibrationEnabled && !props.drawingFeatureType) {
      props.onSelectFeature(undefined);
      return;
    }
    const rect = event.currentTarget.getBoundingClientRect();
    const point = screenToScenePoint(
      { x: event.clientX - rect.left, y: event.clientY - rect.top },
      { width: rect.width, height: rect.height },
      props.viewport,
      { canvasWidth: props.width, canvasHeight: props.height },
    );
    props.onCanvasPointPick(point);
  }}
>
```

Implementation note:

- This pass does **not** require `clampPan` boundary limiting as a hard gate.
- `适配画布` and `重置视图` are the recovery path if the scene is dragged away from the default center.
- During Task 6 manual verification, explicitly check whether free panning makes the scene effectively unrecoverable; if it does, add a small `clampPan` helper to `sceneViewport.ts` and cover it with a viewport unit test before closing the task.

- [ ] **Step 6: Manage viewport state and auto-fit in `SceneEditorPage`**

In `frontend/src/pages/admin/scenes/SceneEditorPage.tsx`:

```tsx
const [viewport, setViewport] = useState(() => createViewportState());
```

Add an `autoFitViewport` helper:

```tsx
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
}, [draft.background, draft.canvasWidth, draft.canvasHeight, draft.features]);
```

Run it on content changes:

```tsx
useEffect(() => {
  if (loading) return;
  autoFitViewport();
}, [loading, autoFitViewport]);
```

Wire controls:

```tsx
<SceneToolbar
  // existing props...
  viewportControls={
    <SceneViewportControls
      onFit={autoFitViewport}
      onZoomIn={() => setViewport((current) => zoomViewport(current, current.zoom * 1.2))}
      onZoomOut={() => setViewport((current) => zoomViewport(current, current.zoom / 1.2))}
      onReset={autoFitViewport}
    />
  }
/>
```

Do not add wheel zoom in the first implementation pass. If the button-based zoom plus drag workflow proves clearly insufficient during Task 6 browser verification, add a small desktop-only `onWheel` handler guarded by `preventDefault`, keep it inside the existing `minZoom/maxZoom` range, and re-run the viewport tests before closing the task.

- [ ] **Step 7: Stabilize the right sidebar width and truncate long values**

In `SceneEditorPage.tsx`, change the right column wrapper:

```tsx
<Col xs={24} lg={5} style={{ minWidth: 320 }}>
  <Space direction="vertical" size={16} style={{ width: '100%' }}>
```

Replace raw values in the draft summary card:

```tsx
<Descriptions.Item label="底图文件">
  <Typography.Text ellipsis={{ tooltip: backgroundName || undefined }}>
    {backgroundName || '未上传'}
  </Typography.Text>
</Descriptions.Item>
<Descriptions.Item label="当前修订">
  <Typography.Text ellipsis={{ tooltip: draft.revisionId || undefined }}>
    {draft.revisionId ? `${draft.revisionId.slice(0, 8)}...${draft.revisionId.slice(-4)}` : '未生成'}
  </Typography.Text>
</Descriptions.Item>
```

In `SceneInspectorPanel.tsx`, make the empty-state form width stable:

```tsx
<Form layout="vertical" style={{ width: '100%' }}>
```

- [ ] **Step 8: Run tests and build**

Run:

```powershell
cd frontend
npm test -- sceneUploadValidation.test.ts sceneViewport.test.ts sceneEditorState.test.ts sceneEditorDrawing.test.ts
npm run build
```

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add frontend/src/app/sceneViewport.ts frontend/src/app/sceneViewport.test.ts frontend/src/pages/admin/scenes/SceneViewportControls.tsx frontend/src/pages/admin/scenes/SceneToolbar.tsx frontend/src/pages/admin/scenes/SceneCanvas.tsx frontend/src/pages/admin/scenes/SceneEditorPage.tsx frontend/src/pages/admin/scenes/SceneInspectorPanel.tsx
git commit -m "feat: add editor viewport controls for real floorplans"
```

---

### Task 5: Read-Only Review Preview Viewport With Real Background

**Files:**
- Modify: `frontend/src/app/types/scene.ts`
- Modify: `frontend/src/app/sceneApi.ts`
- Modify: `frontend/src/pages/exhibitor/selections/SceneSelectionMap.tsx`
- Modify: `frontend/src/pages/admin/selections/SelectionScenePreview.tsx`

- [ ] **Step 1: Write the failing published-scene API type test**

Create a focused type-level runtime test in `frontend/src/app/sceneViewport.test.ts` for fitting full-canvas previews:

```ts
it('fits the full published canvas when a background exists', () => {
  const viewport = fitViewportToBounds(
    { minX: 0, minY: 0, width: 1200, height: 800 },
    { canvasWidth: 1200, canvasHeight: 800 },
  );

  const viewBox = toViewBox(viewport, 1200, 800);
  expect(viewBox.minX).toBeLessThanOrEqual(0);
  expect(viewBox.minY).toBeLessThanOrEqual(0);
});
```

- [ ] **Step 2: Run the viewport tests**

Run:

```powershell
cd frontend
npm test -- sceneViewport.test.ts
```

Expected: PASS after Task 4. This anchors the behavior before wiring preview consumers.

- [ ] **Step 3: Extend the scene API with published-scene background data**

In `frontend/src/app/types/scene.ts`, add:

```ts
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
```

In `frontend/src/app/sceneApi.ts`, add:

```ts
export async function getPublishedScene(eventId: string): Promise<PublishedSceneResponse> {
  return apiGet<PublishedSceneResponse>(`/api/events/${encodeURIComponent(eventId)}/scene/published`);
}
```

- [ ] **Step 4: Add read-only viewport support to `SceneSelectionMap`**

Modify `frontend/src/pages/exhibitor/selections/SceneSelectionMap.tsx` so it can optionally render a background and controls:

```tsx
type SceneSelectionMapProps = {
  booths: SceneSelectableBooth[];
  onToggle: (boothId: string, nextSelected: boolean) => void;
  readOnly?: boolean;
  mobile?: boolean;
  title?: string;
  framed?: boolean;
  backgroundUrl?: string | null;
  canvasWidth?: number;
  canvasHeight?: number;
  showViewportControls?: boolean;
};
```

Add local viewport state:

```tsx
const [viewport, setViewport] = useState(() => createViewportState());
const sceneWidth = props.canvasWidth ?? bounds.width;
const sceneHeight = props.canvasHeight ?? bounds.height;
const viewBox = toViewBox(viewport, sceneWidth, sceneHeight);
```

Render:

```tsx
{props.showViewportControls ? (
  <SceneViewportControls
    onFit={fitCurrentContent}
    onZoomIn={() => setViewport((current) => zoomViewport(current, current.zoom * 1.2))}
    onZoomOut={() => setViewport((current) => zoomViewport(current, current.zoom / 1.2))}
    onReset={fitCurrentContent}
  />
) : null}
```

And inside `<svg>`:

```tsx
<svg viewBox={`${viewBox.minX} ${viewBox.minY} ${viewBox.width} ${viewBox.height}`} ...>
  {props.backgroundUrl ? (
    <image
      href={props.backgroundUrl}
      x={0}
      y={0}
      width={sceneWidth}
      height={sceneHeight}
      preserveAspectRatio="xMidYMid meet"
    />
  ) : null}
  {/* existing booth paths */}
</svg>
```

- [ ] **Step 5: Load both background and booth data in `SelectionScenePreview`**

Update `frontend/src/pages/admin/selections/SelectionScenePreview.tsx`:

```tsx
const [publishedScene, setPublishedScene] = useState<PublishedSceneResponse>();
```

Load both API calls together:

```tsx
const [loadedScene, loadedBooths] = await Promise.all([
  getPublishedScene(props.eventId),
  getPublishedSceneBooths(props.eventId),
]);
setPublishedScene(loadedScene);
setBooths(loadedBooths);
```

Render:

```tsx
<SceneSelectionMap
  booths={mapReviewHighlightBooths(booths, props.highlightedIds)}
  onToggle={() => undefined}
  readOnly
  framed={false}
  title="已选展位高亮"
  showViewportControls
  backgroundUrl={publishedScene?.background?.url ?? null}
  canvasWidth={publishedScene?.canvasWidth}
  canvasHeight={publishedScene?.canvasHeight}
/>
```

- [ ] **Step 6: Run the targeted frontend tests and build**

Run:

```powershell
cd frontend
npm test -- sceneViewport.test.ts sceneSelectionViewModels.test.ts
npm run build
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/app/types/scene.ts frontend/src/app/sceneApi.ts frontend/src/pages/exhibitor/selections/SceneSelectionMap.tsx frontend/src/pages/admin/selections/SelectionScenePreview.tsx
git commit -m "feat: add read-only scene viewport for admin review"
```

---

### Task 6: Browser Verification With Real Floorplan

**Files:**
- Verify: `frontend/src/pages/admin/scenes/SceneEditorPage.tsx`
- Verify: `frontend/src/pages/admin/scenes/SceneCanvas.tsx`
- Verify: `frontend/src/pages/admin/selections/SelectionScenePreview.tsx`
- Artifact: `output/playwright/admin-scene-editor-with-real-floorplan.png`
- Artifact: `output/playwright/admin-selection-review-preview.png`

- [ ] **Step 1: Run the focused frontend and backend regression commands**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn "-Dtest=FileUploadControllerTest,SceneControllerTest,ScenePublishServiceTest" test
cd frontend
npm test -- sceneUploadValidation.test.ts sceneViewport.test.ts sceneEditorState.test.ts sceneEditorDrawing.test.ts sceneSelectionViewModels.test.ts
npm run build
```

Expected: PASS.

- [ ] **Step 2: Start the app locally if it is not already running**

Run:

```powershell
cd D:\project\pe
```

Backend:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys'
mvn spring-boot:run
```

Frontend:

```powershell
cd frontend
npm run dev
```

Expected: backend on `http://127.0.0.1:8080`, frontend on `http://127.0.0.1:5173`.

- [ ] **Step 3: Verify real floorplan upload guard in the browser**

Use the existing smoke-test file if present:

```text
D:\project\pe\tmp-smoke\cad\mccormick-west-building-page1-small.png
```

And verify:

```text
1. Admin opens /admin/events/{eventId}/scene
2. Upload button text shows PNG / JPG / SVG / PDF and 10MB limit
3. Uploading a file under 10MB succeeds
4. Uploading a file over 10MB is blocked before request
```

Expected: successful upload for the small PNG, immediate message for the oversized file.

- [ ] **Step 4: Verify editor viewport controls with a real floorplan**

Check this sequence:

```text
1. Open the scene editor with a real background
2. Confirm the first render shows the full floorplan, not a zoomed fragment
3. Click “放大”, then “缩小”, then “重置视图”
4. Drag the canvas while not drawing or calibrating
5. Enter drawing mode and confirm dragging no longer pans the view
```

Expected: controls work, panning is smooth, drawing mode still captures scene points correctly.

Optional performance observation:

```text
1. Upload a high-resolution floorplan close to 8MB
2. Ensure the scene contains roughly 50 booth polygons
3. Drag the canvas repeatedly
4. Zoom in until the scene is around 300% of the initial fit level
```

If obvious stutter appears, record it as a follow-up and consider:

```text
- requestAnimationFrame throttling for drag updates
- display-oriented background downscaling
- avoiding unnecessary React re-renders during pan / zoom
```

- [ ] **Step 5: Verify right sidebar stability**

With a long real file name loaded, confirm:

```text
1. “底图文件” remains on one stable row
2. “当前修订” shows a shortened value
3. Right column width stays readable
4. No per-character vertical wrapping appears
```

Expected: stable sidebar layout.

- [ ] **Step 6: Verify the admin review preview viewport**

Check this sequence:

```text
1. Submit a scene-driven booth selection
2. Open /admin/selections
3. Expand the pending review row
4. Confirm the preview includes the real background if the published scene has one
5. Use “适配画布 / 放大 / 缩小 / 重置视图”
6. Drag the preview map in read-only mode
```

Expected: preview behaves like a read-only viewport, not a static embedded image.

- [ ] **Step 7: Capture screenshots**

Use Playwright CLI or equivalent and save:

```text
output/playwright/admin-scene-editor-with-real-floorplan.png
output/playwright/admin-selection-review-preview.png
```

Expected: screenshots clearly show the real background and viewport controls.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/app/types/scene.ts frontend/src/app/sceneApi.ts frontend/src/pages/admin/scenes frontend/src/pages/admin/selections/SelectionScenePreview.tsx frontend/src/pages/exhibitor/selections/SceneSelectionMap.tsx backend/src/main/resources/application.yml backend/src/test/java/com/sportexpo/exhibitor/file/FileUploadControllerTest.java
git commit -m "feat: harden scene editor for real floorplans"
```

## Self-Review

### Spec coverage

- Upload limit raised to 10MB: covered by Task 1.
- Frontend size hint and pre-upload blocking: covered by Task 2 and Task 4.
- Shared `viewBox`-based viewport model: covered by Task 3.
- Editor controls for fit / zoom / pan / reset: covered by Task 4.
- Read-only preview viewport with background support: covered by Task 5.
- Stable right sidebar with ellipsis and tooltip: covered by Task 4.
- Browser verification against a real floorplan: covered by Task 6.

### Placeholder scan

- No `TODO`, `TBD`, or “similar to previous task” placeholders remain.
- Every code-changing step includes concrete code snippets.
- Every test step includes a concrete command and expected outcome.

### Type consistency

- `ViewportState`, `SceneBounds`, and `toViewBox` are introduced once in Task 3 and reused consistently later.
- `PublishedSceneResponse` is introduced in Task 5 before preview wiring uses it.
- `SceneViewportControls` is introduced in Task 4 before `SceneToolbar` and `SceneSelectionMap` consume it.
