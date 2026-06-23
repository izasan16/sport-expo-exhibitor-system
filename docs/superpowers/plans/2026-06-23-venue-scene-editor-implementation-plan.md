# Venue Scene Editor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add event-level booth source modes, a draft/publish venue scene editor, and visual booth viewing/selection for admins, sales, and exhibitors.

**Architecture:** Extend the existing Spring Boot baseline schema instead of introducing a migration tool. Keep booth transaction truth in `booths`, `booth_selections`, and related tables, while introducing `venue_scenes`, `scene_revisions`, `scene_features`, and `booth_scene_bindings` as the geometry source for `SCENE_DRIVEN` events. On the frontend, keep Ant Design / ProComponents for shells and forms, and isolate Fabric.js canvas editing plus Turf.js geometry checks behind focused scene modules.

**Tech Stack:** Java 17, Spring Boot 3, JPA, PostgreSQL JSONB, React, TypeScript, Ant Design, ProComponents, Fabric.js, Turf.js, Vitest.

---

## File Structure

Backend files to create:

- `backend/src/main/java/com/sportexpo/exhibitor/scene/VenueScene.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneRevision.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneFeature.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/BoothSceneBinding.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneEditLock.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/VenueSceneRepository.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneRevisionRepository.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneFeatureRepository.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/BoothSceneBindingRepository.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneEditLockRepository.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneGeometry.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneGeometryCalculator.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneValidationService.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/ScenePublishService.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneQueryService.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneEditorService.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneController.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/dto/SceneFeaturePayload.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/dto/SceneDraftResponse.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/dto/SceneRevisionResponse.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/dto/SceneValidationIssueResponse.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/dto/SceneValidationResponse.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/dto/SaveSceneDraftRequest.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/dto/PublishSceneRequest.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/dto/PublishedSceneResponse.java`
- `backend/src/main/java/com/sportexpo/exhibitor/scene/dto/PublishedSceneBoothResponse.java`
- `backend/src/test/java/com/sportexpo/exhibitor/scene/SceneGeometryCalculatorTest.java`
- `backend/src/test/java/com/sportexpo/exhibitor/scene/SceneValidationServiceTest.java`
- `backend/src/test/java/com/sportexpo/exhibitor/scene/ScenePublishServiceTest.java`
- `backend/src/test/java/com/sportexpo/exhibitor/scene/SceneControllerSecurityTest.java`

Backend files to modify:

- `backend/src/main/resources/db/schema-foundation.sql`
- `backend/src/main/java/com/sportexpo/exhibitor/event/Event.java`
- `backend/src/main/java/com/sportexpo/exhibitor/event/EventRepository.java`
- `backend/src/main/java/com/sportexpo/exhibitor/event/EventService.java`
- `backend/src/main/java/com/sportexpo/exhibitor/event/EventController.java`
- `backend/src/main/java/com/sportexpo/exhibitor/event/dto/CreateEventRequest.java`
- `backend/src/main/java/com/sportexpo/exhibitor/event/dto/EventResponse.java`
- `backend/src/main/java/com/sportexpo/exhibitor/booth/Booth.java`
- `backend/src/main/java/com/sportexpo/exhibitor/booth/BoothRepository.java`
- `backend/src/main/java/com/sportexpo/exhibitor/booth/BoothService.java`
- `backend/src/main/java/com/sportexpo/exhibitor/booth/dto/BoothResponse.java`
- `backend/src/main/java/com/sportexpo/exhibitor/selection/BoothSelectionService.java`
- `backend/src/main/java/com/sportexpo/exhibitor/selection/BoothSelectionController.java`
- `backend/src/main/java/com/sportexpo/exhibitor/admin/AdminDashboardService.java`
- `backend/src/main/java/com/sportexpo/exhibitor/common/BusinessStatuses.java`
- `backend/src/test/java/com/sportexpo/exhibitor/FoundationApplicationTest.java`
- `backend/src/test/java/com/sportexpo/exhibitor/TestBusinessFixture.java`
- `backend/src/test/java/com/sportexpo/exhibitor/security/SecurityIntegrationTest.java`
- `backend/pom.xml`

Frontend files to create:

- `frontend/src/app/types/scene.ts`
- `frontend/src/app/sceneGeometry.ts`
- `frontend/src/app/sceneGeometry.test.ts`
- `frontend/src/app/sceneApi.ts`
- `frontend/src/pages/admin/scenes/SceneEditorPage.tsx`
- `frontend/src/pages/admin/scenes/SceneCanvas.tsx`
- `frontend/src/pages/admin/scenes/SceneToolbar.tsx`
- `frontend/src/pages/admin/scenes/SceneLayersPanel.tsx`
- `frontend/src/pages/admin/scenes/SceneInspectorPanel.tsx`
- `frontend/src/pages/admin/scenes/SceneRevisionDrawer.tsx`
- `frontend/src/pages/admin/scenes/sceneEditorState.ts`
- `frontend/src/pages/admin/scenes/sceneEditorState.test.ts`
- `frontend/src/pages/exhibitor/selections/SceneSelectionMap.tsx`
- `frontend/src/pages/exhibitor/selections/sceneSelectionViewModels.ts`
- `frontend/src/pages/exhibitor/selections/sceneSelectionViewModels.test.ts`
- `frontend/src/pages/admin/selections/SelectionScenePreview.tsx`

Frontend files to modify:

- `frontend/package.json`
- `frontend/src/app/domain.ts`
- `frontend/src/app/routes.tsx`
- `frontend/src/app/layouts/AdminLayout.tsx`
- `frontend/src/pages/admin/events/EventListPage.tsx`
- `frontend/src/pages/admin/booths/BoothListPage.tsx`
- `frontend/src/pages/admin/selections/SelectionReviewPage.tsx`
- `frontend/src/pages/exhibitor/selections/BoothSelectionPage.tsx`
- `frontend/src/pages/exhibitor/pageLogic.ts`
- `frontend/src/pages/exhibitor/pageLogic.test.ts`

---

### Task 1: Schema Baseline And Event Mode

**Files:**
- Modify: `backend/src/main/resources/db/schema-foundation.sql`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/event/Event.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/event/EventService.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/event/EventController.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/event/dto/CreateEventRequest.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/event/dto/EventResponse.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/FoundationApplicationTest.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/event/EventServiceTest.java`

- [ ] **Step 1: Write the failing schema test for scene tables and event mode columns**

Add this test to `backend/src/test/java/com/sportexpo/exhibitor/FoundationApplicationTest.java`:

```java
@Test
void createsVenueSceneTablesAndEventModeColumns() {
    var sceneTables = jdbcTemplate.queryForObject("""
            select count(*)
            from information_schema.tables
            where table_schema = ?
              and table_name in ('venue_scenes', 'scene_revisions', 'scene_features', 'booth_scene_bindings', 'scene_edit_locks')
            """, Integer.class, schema);

    var eventColumns = jdbcTemplate.queryForObject("""
            select count(*)
            from information_schema.columns
            where table_schema = ?
              and table_name = 'events'
              and column_name in ('booth_source_mode', 'visual_scene_enabled')
            """, Integer.class, schema);

    assertThat(sceneTables).isEqualTo(5);
    assertThat(eventColumns).isEqualTo(2);
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn -Dtest=FoundationApplicationTest#createsVenueSceneTablesAndEventModeColumns test
```

Expected: FAIL because the new tables and columns do not exist.

- [ ] **Step 3: Extend `schema-foundation.sql` with event mode and scene tables**

Add these DDL fragments to `backend/src/main/resources/db/schema-foundation.sql`:

```sql
alter table __schema__.events
    add column if not exists booth_source_mode varchar(40) not null default 'FORM_ONLY';

alter table __schema__.events
    add column if not exists visual_scene_enabled boolean not null default false;

create table if not exists __schema__.venue_scenes (
    id uuid primary key,
    event_id uuid not null unique references __schema__.events(id),
    status varchar(40) not null,
    canvas_width int not null,
    canvas_height int not null,
    pixel_to_meter_ratio numeric(14,4) not null,
    published_revision_id uuid,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.scene_revisions (
    id uuid primary key,
    scene_id uuid not null references __schema__.venue_scenes(id),
    revision_no int not null,
    status varchar(40) not null,
    change_summary varchar(500),
    snapshot_json jsonb not null,
    published_at timestamptz,
    published_by uuid,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid,
    unique(scene_id, revision_no)
);

create table if not exists __schema__.scene_features (
    id uuid primary key,
    scene_revision_id uuid not null references __schema__.scene_revisions(id),
    event_id uuid not null references __schema__.events(id),
    feature_type varchar(40) not null,
    geometry_type varchar(40) not null,
    geometry_json jsonb not null,
    label varchar(200),
    code varchar(80),
    layer_order int not null default 0,
    style_json jsonb not null default '{}'::jsonb,
    meta_json jsonb not null default '{}'::jsonb,
    area_px2 numeric(14,2),
    area_sqm numeric(14,2),
    locked boolean not null default false,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists __schema__.booth_scene_bindings (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    scene_revision_id uuid not null references __schema__.scene_revisions(id),
    scene_feature_id uuid not null references __schema__.scene_features(id),
    booth_id uuid not null references __schema__.booths(id),
    binding_status varchar(40) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    unique(scene_feature_id),
    unique(booth_id)
);

create table if not exists __schema__.scene_edit_locks (
    event_id uuid primary key references __schema__.events(id),
    revision_id uuid not null references __schema__.scene_revisions(id),
    locked_by uuid not null,
    locked_by_name varchar(160) not null,
    heartbeat_at timestamptz not null,
    expires_at timestamptz not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);
```

- [ ] **Step 4: Add indexes that the scene queries need**

Append these indexes to `backend/src/main/resources/db/schema-foundation.sql`:

```sql
create index if not exists idx_scene_revisions_scene_status
    on __schema__.scene_revisions(scene_id, status, revision_no desc);

create index if not exists idx_scene_features_revision_type
    on __schema__.scene_features(scene_revision_id, feature_type, layer_order);

create index if not exists idx_booth_scene_bindings_event_revision
    on __schema__.booth_scene_bindings(event_id, scene_revision_id);
```

- [ ] **Step 5: Add failing event service test for booth source mode defaults**

Add this test to `backend/src/test/java/com/sportexpo/exhibitor/event/EventServiceTest.java`:

```java
@Test
void createsFormOnlyEventByDefault() {
    var response = service.create(new CreateEventRequest("2027体博会", 2027, null, null));

    assertThat(response.boothSourceMode()).isEqualTo("FORM_ONLY");
    assertThat(response.visualSceneEnabled()).isFalse();
}
```

- [ ] **Step 6: Run the event service test and confirm it fails**

Run:

```powershell
mvn -Dtest=EventServiceTest#createsFormOnlyEventByDefault test
```

Expected: FAIL because `CreateEventRequest` and `EventResponse` do not carry the new fields.

- [ ] **Step 7: Extend the event model, DTOs, and service**

Update the relevant files to match this shape:

```java
public record CreateEventRequest(
        @NotBlank String name,
        @Min(2000) int year,
        String boothSourceMode,
        Boolean visualSceneEnabled
) {}

public record EventResponse(
        UUID id,
        String name,
        int year,
        String status,
        String boothSourceMode,
        boolean visualSceneEnabled
) {}
```

And in `EventService.create`:

```java
var boothSourceMode = request.boothSourceMode() == null || request.boothSourceMode().isBlank()
        ? "FORM_ONLY"
        : request.boothSourceMode();
var visualSceneEnabled = "SCENE_DRIVEN".equals(boothSourceMode);
var event = repository.save(new Event(
        UUID.randomUUID(),
        request.name(),
        request.year(),
        "DRAFT",
        1440,
        boothSourceMode,
        visualSceneEnabled,
        now,
        now
));
```

- [ ] **Step 8: Run both tests and verify they pass**

Run:

```powershell
mvn -Dtest=FoundationApplicationTest#createsVenueSceneTablesAndEventModeColumns,EventServiceTest#createsFormOnlyEventByDefault test
```

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/resources/db/schema-foundation.sql backend/src/main/java/com/sportexpo/exhibitor/event backend/src/test/java/com/sportexpo/exhibitor/FoundationApplicationTest.java backend/src/test/java/com/sportexpo/exhibitor/event/EventServiceTest.java
git commit -m "feat: add venue scene schema baseline"
```

### Task 2: Geometry Core And Scene Domain

**Files:**
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneGeometry.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneGeometryCalculator.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/VenueScene.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneRevision.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneFeature.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/BoothSceneBinding.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneEditLock.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/*Repository.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/scene/SceneGeometryCalculatorTest.java`

- [ ] **Step 1: Write the failing geometry unit test**

Create `backend/src/test/java/com/sportexpo/exhibitor/scene/SceneGeometryCalculatorTest.java`:

```java
class SceneGeometryCalculatorTest {
    private final SceneGeometryCalculator calculator = new SceneGeometryCalculator();

    @Test
    void calculatesClosedPolygonAreaInSquareMeters() {
        var geometry = SceneGeometry.polygon(List.of(
                new BigDecimal("0"), new BigDecimal("0"),
                new BigDecimal("20"), new BigDecimal("0"),
                new BigDecimal("20"), new BigDecimal("20"),
                new BigDecimal("0"), new BigDecimal("20"),
                new BigDecimal("0"), new BigDecimal("0")
        ));

        var area = calculator.calculateAreaSqm(geometry, new BigDecimal("20"));
        assertThat(area).isEqualByComparingTo("1.00");
    }
}
```

- [ ] **Step 2: Run the geometry test and verify it fails**

Run:

```powershell
mvn -Dtest=SceneGeometryCalculatorTest test
```

Expected: FAIL because the calculator and geometry classes do not exist.

- [ ] **Step 3: Implement the geometry core**

Create `SceneGeometry.java` and `SceneGeometryCalculator.java` with this minimal API:

```java
public record SceneGeometry(String type, List<List<BigDecimal>> coordinates) {
    public static SceneGeometry polygon(List<BigDecimal> flatPoints) {
        var points = new ArrayList<List<BigDecimal>>();
        for (int index = 0; index < flatPoints.size(); index += 2) {
            points.add(List.of(flatPoints.get(index), flatPoints.get(index + 1)));
        }
        return new SceneGeometry("Polygon", points);
    }
}

@Component
public class SceneGeometryCalculator {
    public BigDecimal calculateAreaSqm(SceneGeometry geometry, BigDecimal pixelToMeterRatio) {
        var coordinates = geometry.coordinates();
        BigDecimal twiceArea = BigDecimal.ZERO;
        for (int index = 0; index < coordinates.size() - 1; index++) {
            var current = coordinates.get(index);
            var next = coordinates.get(index + 1);
            twiceArea = twiceArea.add(current.get(0).multiply(next.get(1)))
                    .subtract(next.get(0).multiply(current.get(1)));
        }
        var areaPx2 = twiceArea.abs().divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        var ratioSquared = pixelToMeterRatio.multiply(pixelToMeterRatio);
        return areaPx2.divide(ratioSquared, 2, RoundingMode.HALF_UP);
    }
}
```

- [ ] **Step 4: Create the JPA entities and repositories**

Use the schema fields directly in the entities. For example, `VenueScene.java` should start like this:

```java
@Entity
@Table(name = "venue_scenes")
public class VenueScene {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "status")
    private String status;

    @Column(name = "canvas_width")
    private int canvasWidth;

    @Column(name = "canvas_height")
    private int canvasHeight;

    @Column(name = "pixel_to_meter_ratio")
    private BigDecimal pixelToMeterRatio;
}
```

Create repositories with focused queries, for example:

```java
public interface SceneRevisionRepository extends JpaRepository<SceneRevision, UUID> {
    List<SceneRevision> findBySceneIdOrderByRevisionNoDesc(UUID sceneId);
    Optional<SceneRevision> findFirstBySceneIdAndStatusOrderByRevisionNoDesc(UUID sceneId, String status);
}
```

- [ ] **Step 5: Run the geometry test and verify it passes**

Run:

```powershell
mvn -Dtest=SceneGeometryCalculatorTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/sportexpo/exhibitor/scene backend/src/test/java/com/sportexpo/exhibitor/scene/SceneGeometryCalculatorTest.java
git commit -m "feat: add scene geometry core"
```

### Task 3: Draft Validation And Edit Lock

**Files:**
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneValidationService.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneEditorService.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/dto/SceneFeaturePayload.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/dto/SaveSceneDraftRequest.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/dto/SceneValidationIssueResponse.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/dto/SceneValidationResponse.java`
- Create: `backend/src/test/java/com/sportexpo/exhibitor/scene/SceneValidationServiceTest.java`
- Modify: `backend/src/test/java/com/sportexpo/exhibitor/TestBusinessFixture.java`

- [ ] **Step 1: Write the failing validation test**

Create `backend/src/test/java/com/sportexpo/exhibitor/scene/SceneValidationServiceTest.java`:

```java
class SceneValidationServiceTest extends TestDatabaseSupport {
    @Autowired
    private SceneValidationService service;

    @Autowired
    private TestBusinessFixture fixture;

    @Test
    void returnsIssueWhenBoothFallsOutsideHallBoundary() {
        var draft = fixture.sceneDraftWithOutsideBooth();

        var result = service.validateDraft(draft);

        assertThat(result.valid()).isFalse();
        assertThat(result.issues()).extracting(SceneValidationIssueResponse::code)
                .contains("BOOTH_OUTSIDE_HALL");
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run:

```powershell
mvn -Dtest=SceneValidationServiceTest#returnsIssueWhenBoothFallsOutsideHallBoundary test
```

Expected: FAIL because `SceneValidationService` and the fixture helper do not exist.

- [ ] **Step 3: Add scene draft fixture helpers**

Extend `backend/src/test/java/com/sportexpo/exhibitor/TestBusinessFixture.java` with helpers like:

```java
public SaveSceneDraftRequest sceneDraftWithOutsideBooth() {
    return new SaveSceneDraftRequest(
            1200,
            800,
            new BigDecimal("20"),
            List.of(
                    hallBoundaryFeature(),
                    boothFeature("A01", polygon(50, 50, 250, 50, 250, 250, 50, 250, 50, 50)),
                    boothFeature("OUT01", polygon(1100, 700, 1400, 700, 1400, 980, 1100, 980, 1100, 700))
            )
    );
}
```

- [ ] **Step 4: Implement validation and lock services**

Implement `SceneValidationService` with these checks:

```java
public SceneValidationResponse validateDraft(SaveSceneDraftRequest request) {
    var issues = new ArrayList<SceneValidationIssueResponse>();
    var hallBoundary = findSingleHallBoundary(request.features(), issues);
    for (var feature : request.features()) {
        if (!"BOOTH".equals(feature.featureType())) continue;
        if (!isClosedPolygon(feature.geometry())) {
            issues.add(issue(feature.code(), "BOOTH_NOT_CLOSED", "Booth polygon must be closed"));
            continue;
        }
        if (!isInsideHall(feature.geometry(), hallBoundary)) {
            issues.add(issue(feature.code(), "BOOTH_OUTSIDE_HALL", "Booth must stay inside hall boundary"));
        }
    }
    return new SceneValidationResponse(issues.isEmpty(), issues);
}
```

Implement `SceneEditorService.acquireLock` with this behavior:

```java
if (existingLock.isPresent() && existingLock.get().getExpiresAt().isAfter(now) && !existingLock.get().getLockedBy().equals(actorId)) {
    throw new BusinessException("SCENE_LOCKED_BY_OTHER_EDITOR");
}
```

- [ ] **Step 5: Run the validation test and verify it passes**

Run:

```powershell
mvn -Dtest=SceneValidationServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/sportexpo/exhibitor/scene backend/src/test/java/com/sportexpo/exhibitor/scene/SceneValidationServiceTest.java backend/src/test/java/com/sportexpo/exhibitor/TestBusinessFixture.java
git commit -m "feat: add scene draft validation and edit locking"
```

### Task 4: Publish Pipeline And Booth Sync

**Files:**
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/ScenePublishService.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/dto/PublishSceneRequest.java`
- Create: `backend/src/test/java/com/sportexpo/exhibitor/scene/ScenePublishServiceTest.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/booth/Booth.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/booth/BoothRepository.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/booth/BoothService.java`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/common/BusinessStatuses.java`

- [ ] **Step 1: Write the failing publish test**

Create `backend/src/test/java/com/sportexpo/exhibitor/scene/ScenePublishServiceTest.java`:

```java
class ScenePublishServiceTest extends TestDatabaseSupport {
    @Autowired
    private ScenePublishService service;

    @Autowired
    private TestBusinessFixture fixture;

    @Autowired
    private BoothRepository boothRepository;

    @Test
    void publishCreatesAndBindsBoothsForSceneDrivenEvent() {
        var context = fixture.sceneDrivenEventWithDraft();

        var response = service.publish(context.eventId(), new PublishSceneRequest("initial publish"), DemoActors.DEMO_ADMIN_ID);

        assertThat(response.status()).isEqualTo("PUBLISHED");
        assertThat(boothRepository.findByEventIdOrderByBoothNoAsc(context.eventId())).hasSize(2);
    }
}
```

- [ ] **Step 2: Run the publish test and verify it fails**

Run:

```powershell
mvn -Dtest=ScenePublishServiceTest#publishCreatesAndBindsBoothsForSceneDrivenEvent test
```

Expected: FAIL because publish logic and fixture data do not exist.

- [ ] **Step 3: Add publish service with atomic sync**

Implement `ScenePublishService.publish` with this transaction order:

```java
@Transactional
public SceneDraftResponse publish(UUID eventId, PublishSceneRequest request, UUID actorId) {
    var event = eventRepository.findById(eventId).orElseThrow(() -> new BusinessException("EVENT_NOT_FOUND"));
    requireSceneDriven(event);
    var draft = loadDraftRevisionForUpdate(eventId);
    validationService.requirePublishable(draft);
    rejectIfEventHasLockedOrPendingBooths(eventId);
    var scene = sceneRepository.findByEventId(eventId).orElseThrow(() -> new BusinessException("SCENE_NOT_FOUND"));
    syncBoothsFromDraft(eventId, draft, actorId);
    markRevisionPublished(draft, actorId);
    scene.markPublished(draft.getId(), actorId, OffsetDateTime.now());
    lockRepository.deleteById(eventId);
    auditService.record(eventId, actorId, "SCENE_PUBLISH", "VenueScene", scene.getId(), "DRAFT", "PUBLISHED", request.changeSummary());
    return queryService.getDraft(eventId, actorId);
}
```

- [ ] **Step 4: Implement booth sync rules**

Add focused sync behavior in `ScenePublishService`:

```java
private void syncBoothsFromDraft(UUID eventId, SceneRevision draft, UUID actorId) {
    var boothFeatures = featureRepository.findBySceneRevisionIdAndFeatureTypeOrderByLayerOrderAsc(draft.getId(), "BOOTH");
    var existingBooths = boothRepository.findByEventIdOrderByBoothNoAsc(eventId);
    var existingByNo = existingBooths.stream().collect(Collectors.toMap(Booth::getBoothNo, Function.identity()));

    for (var feature : boothFeatures) {
        var boothNo = feature.metaText("boothNo");
        var areaSqm = feature.requireAreaSqm();
        var boothType = feature.metaText("boothType");
        var zoneId = feature.metaUuid("zoneId");
        var cachedBounds = feature.calculateBounds();
        var booth = existingByNo.get(boothNo);
        if (booth == null) {
            boothRepository.save(Booth.sceneDriven(UUID.randomUUID(), eventId, zoneId, boothNo, areaSqm, boothType, cachedBounds, actorId));
        } else {
            booth.applySceneGeometry(areaSqm, zoneId, cachedBounds, actorId);
        }
    }
}
```

- [ ] **Step 5: Run the publish test and verify it passes**

Run:

```powershell
mvn -Dtest=ScenePublishServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/sportexpo/exhibitor/scene backend/src/main/java/com/sportexpo/exhibitor/booth backend/src/main/java/com/sportexpo/exhibitor/common/BusinessStatuses.java backend/src/test/java/com/sportexpo/exhibitor/scene/ScenePublishServiceTest.java backend/src/test/java/com/sportexpo/exhibitor/TestBusinessFixture.java
git commit -m "feat: add scene publish pipeline"
```

### Task 5: Scene APIs And Security

**Files:**
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneController.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/SceneQueryService.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/dto/SceneDraftResponse.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/dto/SceneRevisionResponse.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/dto/PublishedSceneResponse.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/scene/dto/PublishedSceneBoothResponse.java`
- Create: `backend/src/test/java/com/sportexpo/exhibitor/scene/SceneControllerSecurityTest.java`
- Modify: `backend/src/test/java/com/sportexpo/exhibitor/security/SecurityIntegrationTest.java`

- [ ] **Step 1: Write the failing security test**

Create `backend/src/test/java/com/sportexpo/exhibitor/scene/SceneControllerSecurityTest.java`:

```java
class SceneControllerSecurityTest extends TestDatabaseSupport {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void exhibitorCanReadPublishedSceneButCannotEditDraft() throws Exception {
        mockMvc.perform(get("/api/events/{eventId}/scene/published", OTHER_EVENT_ID)
                        .header("Authorization", "Bearer " + login("exhibitor01")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/events/{eventId}/scene/draft/validate", OTHER_EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canvasWidth\":1200,\"canvasHeight\":800,\"pixelToMeterRatio\":20,\"features\":[]}")
                        .header("Authorization", "Bearer " + login("exhibitor01")))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Run the security test and verify it fails**

Run:

```powershell
mvn -Dtest=SceneControllerSecurityTest test
```

Expected: FAIL because the scene controller does not exist.

- [ ] **Step 3: Add controller endpoints with role boundaries**

Implement `SceneController.java` with these mappings:

```java
@RestController
@RequestMapping("/api/events/{eventId}/scene")
public class SceneController {
    @GetMapping("/published")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES','EXHIBITOR')")
    public ApiResponse<PublishedSceneResponse> getPublishedScene(@PathVariable UUID eventId) { ... }

    @GetMapping("/published/booths")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN','SALES','EXHIBITOR')")
    public ApiResponse<List<PublishedSceneBoothResponse>> getPublishedSceneBooths(@PathVariable UUID eventId) { ... }

    @PostMapping("/draft")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN')")
    public ApiResponse<SceneDraftResponse> saveDraft(@PathVariable UUID eventId, @Valid @RequestBody SaveSceneDraftRequest request) { ... }

    @PostMapping("/draft/validate")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN')")
    public ApiResponse<SceneValidationResponse> validateDraft(@PathVariable UUID eventId, @Valid @RequestBody SaveSceneDraftRequest request) { ... }

    @PostMapping("/draft/publish")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN')")
    public ApiResponse<SceneDraftResponse> publish(@PathVariable UUID eventId, @Valid @RequestBody PublishSceneRequest request) { ... }

    @GetMapping("/revisions")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN')")
    public ApiResponse<List<SceneRevisionResponse>> listRevisions(@PathVariable UUID eventId) { ... }

    @GetMapping("/revisions/{revisionId}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ORGANIZER_ADMIN')")
    public ApiResponse<SceneDraftResponse> getRevision(@PathVariable UUID eventId, @PathVariable UUID revisionId) { ... }
}
```

- [ ] **Step 4: Add published-scene query DTO shape**

Use this response shape so the viewer gets both geometry and booth status in one call:

```java
public record PublishedSceneBoothResponse(
        UUID boothId,
        String boothNo,
        String boothType,
        String status,
        BigDecimal areaSqm,
        UUID zoneId,
        JsonNode geometry,
        BigDecimal x,
        BigDecimal y,
        BigDecimal width,
        BigDecimal height
) {}
```

- [ ] **Step 5: Run the security test and existing exhibitor area-tier security test**

Run:

```powershell
mvn -Dtest=SceneControllerSecurityTest,SecurityIntegrationTest#exhibitorCanReadAreaTiersForEventSelectionAndOrderDetail test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/sportexpo/exhibitor/scene backend/src/test/java/com/sportexpo/exhibitor/scene/SceneControllerSecurityTest.java backend/src/test/java/com/sportexpo/exhibitor/security/SecurityIntegrationTest.java
git commit -m "feat: add venue scene APIs"
```

### Task 6: Frontend Dependencies, Types, And Event Admin Integration

**Files:**
- Modify: `frontend/package.json`
- Create: `frontend/src/app/types/scene.ts`
- Create: `frontend/src/app/sceneApi.ts`
- Modify: `frontend/src/app/domain.ts`
- Modify: `frontend/src/app/routes.tsx`
- Modify: `frontend/src/app/layouts/AdminLayout.tsx`
- Modify: `frontend/src/pages/admin/events/EventListPage.tsx`
- Modify: `frontend/src/pages/admin/booths/BoothListPage.tsx`
- Test: `frontend/src/app/sceneGeometry.test.ts`

- [ ] **Step 1: Write the failing frontend geometry test**

Create `frontend/src/app/sceneGeometry.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { polygonAreaSqm } from './sceneGeometry';

describe('polygonAreaSqm', () => {
  it('converts pixel area into square meters', () => {
    expect(
      polygonAreaSqm(
        [[0, 0], [20, 0], [20, 20], [0, 20], [0, 0]],
        20,
      ),
    ).toBe('1.00');
  });
});
```

- [ ] **Step 2: Run the test and verify it fails**

Run:

```powershell
cd frontend
npm test -- sceneGeometry.test.ts
```

Expected: FAIL because the helper does not exist.

- [ ] **Step 3: Add the frontend dependencies**

Update `frontend/package.json` dependencies:

```json
"dependencies": {
  "@ant-design/icons": "^5.5.1",
  "@ant-design/pro-components": "^2.8.1",
  "@turf/turf": "^7.2.0",
  "antd": "^5.21.6",
  "fabric": "^6.5.3",
  "react": "^18.3.1",
  "react-dom": "^18.3.1",
  "react-router-dom": "^6.27.0"
}
```

- [ ] **Step 4: Add scene types and API wrappers**

Create `frontend/src/app/types/scene.ts` and `frontend/src/app/sceneApi.ts` with these minimal signatures:

```ts
export type SceneFeatureType = 'HALL_BOUNDARY' | 'AISLE' | 'OBSTACLE' | 'WALL' | 'ENTRANCE' | 'SERVICE_POINT' | 'ZONE_BOUNDARY' | 'BOOTH';

export type SceneFeaturePayload = {
  id: string;
  featureType: SceneFeatureType;
  geometryType: 'POLYGON' | 'POLYLINE' | 'POINT';
  geometry: GeoJSON.Geometry;
  label?: string;
  code?: string;
  layerOrder: number;
  meta: Record<string, unknown>;
};

export async function getPublishedScene(eventId: string) {
  return apiGet<PublishedSceneResponse>(`/api/events/${encodeURIComponent(eventId)}/scene/published`);
}
```

- [ ] **Step 5: Update admin routes and event page**

In `frontend/src/app/routes.tsx`, add:

```tsx
const SceneEditorPage = lazy(() => import('../pages/admin/scenes/SceneEditorPage'));
...
{ path: 'events/:eventId/scene', element: lazyPage(<SceneEditorPage />) },
```

In `frontend/src/pages/admin/events/EventListPage.tsx`, extend the row type and add mode-aware actions:

```ts
type EventRow = {
  id: string;
  name: string;
  year: number;
  status: string;
  boothSourceMode: 'FORM_ONLY' | 'SCENE_DRIVEN';
  visualSceneEnabled: boolean;
};
```

Render an action column:

```tsx
{
  title: '操作',
  valueType: 'option',
  render: (_, record) => record.boothSourceMode === 'SCENE_DRIVEN'
    ? [<Button key="scene" type="link" onClick={() => navigate(`/admin/events/${record.id}/scene`)}>编辑馆图</Button>]
    : [],
}
```

- [ ] **Step 6: Make `BoothListPage` mode-aware**

If the selected event is `SCENE_DRIVEN`, show:

```tsx
<Result
  status="info"
  title="该展会使用可视化场景模式"
  subTitle="请进入馆图编辑器维护 Booth。"
  extra={<Button type="primary" onClick={() => navigate(`/admin/events/${eventId}/scene`)}>打开馆图编辑器</Button>}
/>
```

- [ ] **Step 7: Implement `sceneGeometry.ts` and run the test**

Create:

```ts
export function polygonAreaSqm(points: number[][], pixelToMeterRatio: number): string {
  let twiceArea = 0;
  for (let index = 0; index < points.length - 1; index += 1) {
    const [x1, y1] = points[index];
    const [x2, y2] = points[index + 1];
    twiceArea += x1 * y2 - x2 * y1;
  }
  const areaPx2 = Math.abs(twiceArea) / 2;
  const areaSqm = areaPx2 / (pixelToMeterRatio * pixelToMeterRatio);
  return areaSqm.toFixed(2);
}
```

Run:

```powershell
cd frontend
npm test -- sceneGeometry.test.ts
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add frontend/package.json frontend/src/app frontend/src/app/routes.tsx frontend/src/app/layouts/AdminLayout.tsx frontend/src/pages/admin/events/EventListPage.tsx frontend/src/pages/admin/booths/BoothListPage.tsx
git commit -m "feat: add frontend scene foundations"
```

### Task 7: Admin Scene Editor Shell

**Files:**
- Create: `frontend/src/pages/admin/scenes/SceneEditorPage.tsx`
- Create: `frontend/src/pages/admin/scenes/SceneCanvas.tsx`
- Create: `frontend/src/pages/admin/scenes/SceneToolbar.tsx`
- Create: `frontend/src/pages/admin/scenes/SceneLayersPanel.tsx`
- Create: `frontend/src/pages/admin/scenes/SceneInspectorPanel.tsx`
- Create: `frontend/src/pages/admin/scenes/sceneEditorState.ts`
- Create: `frontend/src/pages/admin/scenes/sceneEditorState.test.ts`

- [ ] **Step 1: Write the failing editor-state test**

Create `frontend/src/pages/admin/scenes/sceneEditorState.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { createEmptySceneDraft, insertFeature, toggleSelectedFeature } from './sceneEditorState';

describe('sceneEditorState', () => {
  it('adds a feature and selects it', () => {
    const draft = createEmptySceneDraft();
    const next = insertFeature(draft, {
      id: 'feature-1',
      featureType: 'BOOTH',
      geometryType: 'POLYGON',
      geometry: { type: 'Polygon', coordinates: [[[0, 0], [20, 0], [20, 20], [0, 20], [0, 0]]] },
      layerOrder: 1,
      meta: { boothNo: 'A01' },
    });

    const selected = toggleSelectedFeature(next, 'feature-1');
    expect(selected.features).toHaveLength(1);
    expect(selected.selectedFeatureId).toBe('feature-1');
  });
});
```

- [ ] **Step 2: Run the test and verify it fails**

Run:

```powershell
cd frontend
npm test -- sceneEditorState.test.ts
```

Expected: FAIL because the editor-state module does not exist.

- [ ] **Step 3: Implement the scene editor state helpers**

Create `sceneEditorState.ts`:

```ts
export function createEmptySceneDraft(): SceneDraftState {
  return {
    canvasWidth: 1200,
    canvasHeight: 800,
    pixelToMeterRatio: 20,
    features: [],
    selectedFeatureId: undefined,
  };
}

export function insertFeature(state: SceneDraftState, feature: SceneFeaturePayload): SceneDraftState {
  return { ...state, features: [...state.features, feature] };
}

export function toggleSelectedFeature(state: SceneDraftState, featureId?: string): SceneDraftState {
  return { ...state, selectedFeatureId: featureId };
}
```

- [ ] **Step 4: Build the editor shell with Ant Design layout**

`SceneEditorPage.tsx` should compose the page like this:

```tsx
<PageContainer title="馆图编辑器">
  <Row gutter={16}>
    <Col span={5}><SceneLayersPanel ... /></Col>
    <Col span={14}><SceneCanvas ... /></Col>
    <Col span={5}><SceneInspectorPanel ... /></Col>
  </Row>
</PageContainer>
```

`SceneToolbar.tsx` should expose buttons for:

```tsx
['保存草稿', '校验', '发布', '撤销', '重做', '适应画布']
```

- [ ] **Step 5: Implement a minimal Fabric-backed canvas**

In `SceneCanvas.tsx`, start with a minimal effect:

```tsx
useEffect(() => {
  const canvas = new fabric.Canvas(canvasRef.current!, { selection: false });
  canvas.setWidth(width);
  canvas.setHeight(height);
  return () => canvas.dispose();
}, [width, height]);
```

Then render booth polygons from the draft state using `new fabric.Polygon(...)`.

- [ ] **Step 6: Run the state test and the frontend build**

Run:

```powershell
cd frontend
npm test -- sceneEditorState.test.ts
npm run build
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/pages/admin/scenes
git commit -m "feat: add admin scene editor shell"
```

### Task 8: Admin Draft Save, Validate, Publish, And Revision History

**Files:**
- Modify: `frontend/src/pages/admin/scenes/SceneEditorPage.tsx`
- Create: `frontend/src/pages/admin/scenes/SceneRevisionDrawer.tsx`
- Modify: `frontend/src/app/sceneApi.ts`
- Modify: `frontend/src/pages/admin/scenes/sceneEditorState.ts`

- [ ] **Step 1: Write the failing revision-view-model test**

Extend `frontend/src/pages/admin/scenes/sceneEditorState.test.ts`:

```ts
it('replaces draft state with validated server payload', () => {
  const state = createEmptySceneDraft();
  const next = applyServerDraft(state, {
    sceneId: 'scene-1',
    revisionId: 'rev-2',
    status: 'DRAFT',
    canvasWidth: 1600,
    canvasHeight: 900,
    pixelToMeterRatio: 25,
    features: [],
  });

  expect(next.sceneId).toBe('scene-1');
  expect(next.revisionId).toBe('rev-2');
  expect(next.canvasWidth).toBe(1600);
});
```

- [ ] **Step 2: Run the test and verify it fails**

Run:

```powershell
cd frontend
npm test -- sceneEditorState.test.ts
```

Expected: FAIL because `applyServerDraft` does not exist.

- [ ] **Step 3: Extend API wrappers**

Add these methods to `frontend/src/app/sceneApi.ts`:

```ts
export async function saveSceneDraft(eventId: string, body: SaveSceneDraftBody) {
  return apiPost<SceneDraftResponse>(`/api/events/${encodeURIComponent(eventId)}/scene/draft`, body);
}

export async function validateSceneDraft(eventId: string, body: SaveSceneDraftBody) {
  return apiPost<SceneValidationResponse>(`/api/events/${encodeURIComponent(eventId)}/scene/draft/validate`, body);
}

export async function publishSceneDraft(eventId: string, changeSummary: string) {
  return apiPost<SceneDraftResponse>(`/api/events/${encodeURIComponent(eventId)}/scene/draft/publish`, { changeSummary });
}

export async function getSceneRevisions(eventId: string) {
  return apiGet<SceneRevisionResponse[]>(`/api/events/${encodeURIComponent(eventId)}/scene/revisions`);
}

export async function getSceneRevision(eventId: string, revisionId: string) {
  return apiGet<SceneDraftResponse>(`/api/events/${encodeURIComponent(eventId)}/scene/revisions/${encodeURIComponent(revisionId)}`);
}
```

- [ ] **Step 4: Add the server-draft state helper**

Implement in `sceneEditorState.ts`:

```ts
export function applyServerDraft(state: SceneDraftState, payload: SceneDraftResponse): SceneDraftState {
  return {
    ...state,
    sceneId: payload.sceneId,
    revisionId: payload.revisionId,
    status: payload.status,
    canvasWidth: payload.canvasWidth,
    canvasHeight: payload.canvasHeight,
    pixelToMeterRatio: payload.pixelToMeterRatio,
    features: payload.features,
  };
}
```

- [ ] **Step 5: Wire save / validate / publish UI**

In `SceneEditorPage.tsx`, add these handlers:

```tsx
const handleSaveDraft = async () => {
  const draft = await saveSceneDraft(eventId, toSaveBody(state));
  setState((current) => applyServerDraft(current, draft));
  message.success('草稿已保存');
};

const handleValidate = async () => {
  const result = await validateSceneDraft(eventId, toSaveBody(state));
  setValidation(result);
  message[result.valid ? 'success' : 'warning'](result.valid ? '场景校验通过' : '场景存在待修正问题');
};

const handlePublish = async () => {
  const draft = await publishSceneDraft(eventId, publishSummary);
  setState((current) => applyServerDraft(current, draft));
  message.success('馆图已发布');
};
```

- [ ] **Step 6: Add revision drawer**

`SceneRevisionDrawer.tsx` should render:

```tsx
<Drawer open={open} title="发布历史" onClose={onClose}>
  <List
    dataSource={revisions}
    renderItem={(item) => (
      <List.Item>
        <List.Item.Meta title={`版本 ${item.revisionNo}`} description={item.changeSummary || '无变更说明'} />
        <Tag color={item.status === 'PUBLISHED' ? 'green' : 'default'}>{item.status}</Tag>
      </List.Item>
    )}
  />
</Drawer>
```

And `SceneEditorPage.tsx` should load revisions on demand:

```tsx
const openRevisionDrawer = async () => {
  const items = await getSceneRevisions(eventId);
  setRevisions(items);
  setRevisionDrawerOpen(true);
};
```

- [ ] **Step 7: Run the state test and the frontend build**

Run:

```powershell
cd frontend
npm test -- sceneEditorState.test.ts
npm run build
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/app/sceneApi.ts frontend/src/pages/admin/scenes
git commit -m "feat: add scene draft and publish workflow"
```

### Task 9: Scene-Driven Exhibitor And Sales Selection View

**Files:**
- Create: `frontend/src/pages/exhibitor/selections/SceneSelectionMap.tsx`
- Create: `frontend/src/pages/exhibitor/selections/sceneSelectionViewModels.ts`
- Create: `frontend/src/pages/exhibitor/selections/sceneSelectionViewModels.test.ts`
- Modify: `frontend/src/pages/exhibitor/selections/BoothSelectionPage.tsx`
- Modify: `frontend/src/pages/exhibitor/pageLogic.ts`
- Modify: `frontend/src/pages/exhibitor/pageLogic.test.ts`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/selection/BoothSelectionService.java`

- [ ] **Step 1: Write the failing selection view-model test**

Create `frontend/src/pages/exhibitor/selections/sceneSelectionViewModels.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { mapSelectableSceneBooths } from './sceneSelectionViewModels';

describe('mapSelectableSceneBooths', () => {
  it('marks selected booth ids and filters out mismatched booth type', () => {
    const result = mapSelectableSceneBooths(
      [
        { boothId: 'b1', boothNo: 'A01', boothType: 'STANDARD', status: 'AVAILABLE', areaSqm: '9.00', geometry: { type: 'Polygon', coordinates: [] } },
        { boothId: 'b2', boothNo: 'B01', boothType: 'CUSTOM', status: 'AVAILABLE', areaSqm: '9.00', geometry: { type: 'Polygon', coordinates: [] } },
      ],
      { boothType: 'STANDARD' },
      ['b1'],
    );

    expect(result).toHaveLength(1);
    expect(result[0].selected).toBe(true);
  });
});
```

- [ ] **Step 2: Run the test and verify it fails**

Run:

```powershell
cd frontend
npm test -- sceneSelectionViewModels.test.ts
```

Expected: FAIL because the scene selection view-model does not exist.

- [ ] **Step 3: Add the scene selection view-model**

Create `sceneSelectionViewModels.ts`:

```ts
export function mapSelectableSceneBooths(
  booths: PublishedSceneBoothResponse[],
  order: { boothType: string },
  selectedBoothIds: string[],
) {
  return booths
    .filter((item) => item.boothType === order.boothType)
    .map((item) => ({
      ...item,
      selected: selectedBoothIds.includes(item.boothId),
      disabled: item.status !== 'AVAILABLE',
    }));
}
```

- [ ] **Step 4: Make the backend selection service accept scene-driven geometry source**

In `BoothSelectionService`, keep the existing business transaction, but branch the read path:

```java
var event = eventRepository.findById(order.getEventId()).orElseThrow(...);
var candidateBooths = "SCENE_DRIVEN".equals(event.getBoothSourceMode())
        ? boothRepository.findByEventIdAndIdInOrderByBoothNoAsc(order.getEventId(), sortedBoothIds)
        : boothRepository.findByEventIdAndIdInOrderByBoothNoAsc(order.getEventId(), sortedBoothIds);
```

Do not alter locking order; still lock booth rows by booth UUID ascending.

- [ ] **Step 5: Update `BoothSelectionPage.tsx` for dual-mode rendering**

After the order loads, branch on event mode:

```tsx
const isSceneDriven = orderContextEvent?.boothSourceMode === 'SCENE_DRIVEN';

const [sceneBooths, setSceneBooths] = useState<PublishedSceneBoothResponse[]>([]);
...
const loadedSceneBooths = isSceneDriven
  ? await getPublishedSceneBooths(loadedOrder.eventId)
  : [];
```

Render:

```tsx
{isSceneDriven ? (
  <SceneSelectionMap
    booths={mappedSceneBooths}
    selectedBoothIds={selectedKeys.map(String)}
    onToggle={toggleBooth}
  />
) : (
  <LegacyBoothTileMap booths={sortedBooths} ... />
)}
```

- [ ] **Step 6: Keep desktop multi-select and mobile single-detail behavior**

Implement these two interactions in `SceneSelectionMap.tsx`:

```tsx
if (isMobile) {
  setFocusedBooth(booth);
} else {
  onToggle(booth.boothId, !booth.selected);
}
```

And in the mobile drawer confirm button:

```tsx
<Button type="primary" disabled={focusedBooth.disabled} onClick={() => onToggle(focusedBooth.boothId, !focusedBooth.selected)}>
  {focusedBooth.selected ? '取消选择' : '加入已选'}
</Button>
```

- [ ] **Step 7: Run the view-model test and frontend build**

Run:

```powershell
cd frontend
npm test -- sceneSelectionViewModels.test.ts pageLogic.test.ts
npm run build
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/pages/exhibitor/selections frontend/src/pages/exhibitor/pageLogic.ts frontend/src/pages/exhibitor/pageLogic.test.ts backend/src/main/java/com/sportexpo/exhibitor/selection/BoothSelectionService.java
git commit -m "feat: add scene-driven exhibitor selection view"
```

### Task 10: Admin Selection Review Scene Preview And Final Verification

**Files:**
- Create: `frontend/src/pages/admin/selections/SelectionScenePreview.tsx`
- Modify: `frontend/src/pages/admin/selections/SelectionReviewPage.tsx`
- Modify: `backend/src/main/java/com/sportexpo/exhibitor/admin/AdminDashboardService.java`
- Modify: `frontend/src/pages/admin/DashboardPage.tsx`

- [ ] **Step 1: Add a failing admin review helper test**

Extend `frontend/src/pages/exhibitor/selections/sceneSelectionViewModels.test.ts` with a second test:

```ts
it('marks reviewed selection booths for admin preview', () => {
  const preview = mapReviewHighlightBooths(
    [
      { boothId: 'b1', boothNo: 'A01', boothType: 'STANDARD', status: 'LOCKED', areaSqm: '9.00', geometry: { type: 'Polygon', coordinates: [] } },
      { boothId: 'b2', boothNo: 'A02', boothType: 'STANDARD', status: 'AVAILABLE', areaSqm: '9.00', geometry: { type: 'Polygon', coordinates: [] } },
    ],
    ['b1'],
  );

  expect(preview.find((item) => item.boothId === 'b1')?.highlighted).toBe(true);
});
```

- [ ] **Step 2: Run the test and verify it fails**

Run:

```powershell
cd frontend
npm test -- sceneSelectionViewModels.test.ts
```

Expected: FAIL because `mapReviewHighlightBooths` does not exist.

- [ ] **Step 3: Implement the review preview helper**

Add to `sceneSelectionViewModels.ts`:

```ts
export function mapReviewHighlightBooths(
  booths: PublishedSceneBoothResponse[],
  highlightedIds: string[],
) {
  return booths.map((item) => ({
    ...item,
    highlighted: highlightedIds.includes(item.boothId),
  }));
}
```

- [ ] **Step 4: Add admin scene preview component**

Create `SelectionScenePreview.tsx`:

```tsx
export default function SelectionScenePreview({ booths, highlightedIds }: Props) {
  const previewBooths = mapReviewHighlightBooths(booths, highlightedIds);
  return (
    <Card title="地图预览" bodyStyle={{ padding: 12 }}>
      <SceneSelectionMap
        booths={previewBooths.map((item) => ({ ...item, selected: item.highlighted, disabled: true }))}
        selectedBoothIds={highlightedIds}
        onToggle={() => undefined}
        readOnly
      />
    </Card>
  );
}
```

- [ ] **Step 5: Embed preview into selection review page**

In `SelectionReviewPage.tsx`, when the selection event is `SCENE_DRIVEN`, load published scene booths and render:

```tsx
<SelectionScenePreview booths={sceneBooths} highlightedIds={record.boothIds} />
```

- [ ] **Step 6: Run build and backend scene test suite**

Run:

```powershell
cd backend
mvn -Dtest=SceneGeometryCalculatorTest,SceneValidationServiceTest,ScenePublishServiceTest,SceneControllerSecurityTest test
cd ..\frontend
npm test -- sceneGeometry.test.ts sceneEditorState.test.ts sceneSelectionViewModels.test.ts
npm run build
```

Expected: PASS.

- [ ] **Step 7: Smoke test in browser**

Run the app and verify these flows manually:

```text
1. Admin creates a SCENE_DRIVEN event
2. Admin opens /admin/events/{eventId}/scene
3. Admin draws hall + two booth polygons, saves, validates, publishes
4. Exhibitor opens /exhibitor/orders/{orderId}/selection and sees the scene map
5. Admin opens /admin/selections and sees highlighted booth preview
```

Expected: all five flows work without 403 or blank canvas.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/pages/admin/selections frontend/src/pages/exhibitor/selections frontend/src/pages/admin/DashboardPage.tsx backend/src/main/java/com/sportexpo/exhibitor/admin/AdminDashboardService.java
git commit -m "feat: add scene review preview and verification"
```

## Self-Review

### Spec coverage

- Event-level `FORM_ONLY` / `SCENE_DRIVEN` mode: covered by Task 1 and Task 6.
- Draft / published dual-version scene model: covered by Task 2, Task 3, Task 4, and Task 8.
- GeoJSON, pixel ratio, area calculation, validation, and lock rules: covered by Task 2 and Task 3.
- Admin scene editor shell and publish workflow: covered by Task 7 and Task 8.
- Exhibitor and sales visual selection: covered by Task 9.
- Admin scene preview in review flow: covered by Task 10.

### Placeholder scan

- No `TODO`, `TBD`, or “implement later” markers remain.
- Each task includes concrete file paths, commands, and expected outputs.

### Type consistency

- Event mode uses `FORM_ONLY` and `SCENE_DRIVEN` consistently across backend and frontend.
- Scene geometry uses GeoJSON consistently.
- Published booth viewer types use `PublishedSceneBoothResponse` consistently in Tasks 5, 9, and 10.
