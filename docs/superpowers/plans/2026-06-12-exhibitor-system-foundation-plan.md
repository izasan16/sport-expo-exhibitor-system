# Exhibitor System Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the runnable foundation for the exhibitor system: React + TypeScript + Ant Design frontend, Spring Boot backend, PostgreSQL schema initialization in a dedicated schema, Redis wiring, RBAC/audit foundations, and core exhibition master data APIs.

**Architecture:** Create a modular monolith backend and a single React frontend with separate exhibitor/admin route groups loaded lazily. PostgreSQL is the source of truth; Redis is wired for cache/TTL use but not used for final business state in this foundation plan. This plan implements the base platform and core master data only; order/contract/payment/selection/service-center flows are implemented in later plans.

**Tech Stack:** React, TypeScript, Vite, Ant Design, ProComponents, Java 17, Spring Boot 3, Maven, PostgreSQL, Redis, JUnit 5, Testcontainers.

---

## File Structure

Create this repository structure:

```text
frontend/
  package.json
  index.html
  tsconfig.json
  vite.config.ts
  src/
    main.tsx
    app/App.tsx
    app/routes.tsx
    app/request.ts
    app/auth.ts
    app/layouts/AdminLayout.tsx
    app/layouts/ExhibitorLayout.tsx
    pages/admin/DashboardPage.tsx
    pages/admin/events/EventListPage.tsx
    pages/admin/booths/BoothListPage.tsx
    pages/exhibitor/HomePage.tsx
    pages/LoginPage.tsx
    styles/global.css

backend/
  pom.xml
  src/main/java/com/sportexpo/exhibitor/ExhibitorApplication.java
  src/main/java/com/sportexpo/exhibitor/config/SchemaInitializer.java
  src/main/java/com/sportexpo/exhibitor/common/ApiResponse.java
  src/main/java/com/sportexpo/exhibitor/common/PageResponse.java
  src/main/java/com/sportexpo/exhibitor/common/BusinessException.java
  src/main/java/com/sportexpo/exhibitor/audit/AuditLog.java
  src/main/java/com/sportexpo/exhibitor/audit/AuditLogRepository.java
  src/main/java/com/sportexpo/exhibitor/audit/AuditService.java
  src/main/java/com/sportexpo/exhibitor/system/User.java
  src/main/java/com/sportexpo/exhibitor/system/Role.java
  src/main/java/com/sportexpo/exhibitor/system/Permission.java
  src/main/java/com/sportexpo/exhibitor/event/Event.java
  src/main/java/com/sportexpo/exhibitor/event/EventController.java
  src/main/java/com/sportexpo/exhibitor/event/EventRepository.java
  src/main/java/com/sportexpo/exhibitor/event/EventService.java
  src/main/java/com/sportexpo/exhibitor/event/dto/CreateEventRequest.java
  src/main/java/com/sportexpo/exhibitor/event/dto/EventResponse.java
  src/main/java/com/sportexpo/exhibitor/booth/Pavilion.java
  src/main/java/com/sportexpo/exhibitor/booth/Zone.java
  src/main/java/com/sportexpo/exhibitor/booth/Booth.java
  src/main/java/com/sportexpo/exhibitor/booth/BoothController.java
  src/main/java/com/sportexpo/exhibitor/booth/BoothRepository.java
  src/main/java/com/sportexpo/exhibitor/booth/BoothService.java
  src/main/java/com/sportexpo/exhibitor/booth/dto/CreateBoothRequest.java
  src/main/java/com/sportexpo/exhibitor/booth/dto/BoothResponse.java
  src/main/resources/application.yml
  src/main/resources/db/schema-foundation.sql
  src/test/java/com/sportexpo/exhibitor/FoundationApplicationTest.java
  src/test/java/com/sportexpo/exhibitor/event/EventServiceTest.java
  src/test/java/com/sportexpo/exhibitor/booth/BoothServiceTest.java
docker-compose.yml
README.md
```

## Task 1: Create Backend Maven Project

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/ExhibitorApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/java/com/sportexpo/exhibitor/FoundationApplicationTest.java`

- [ ] **Step 1: Write the failing application annotation test**

Create `backend/src/test/java/com/sportexpo/exhibitor/FoundationApplicationTest.java`:

```java
package com.sportexpo.exhibitor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;

class FoundationApplicationTest {
    @Test
    void applicationClassIsSpringBootApplication() {
        assertThat(ExhibitorApplication.class.getAnnotation(SpringBootApplication.class)).isNotNull();
    }
}
```

- [ ] **Step 2: Add Maven project**

Create `backend/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>
    <groupId>com.sportexpo</groupId>
    <artifactId>exhibitor-system</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <name>exhibitor-system</name>
    <properties>
        <java.version>17</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Add Spring Boot application**

Create `backend/src/main/java/com/sportexpo/exhibitor/ExhibitorApplication.java`:

```java
package com.sportexpo.exhibitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExhibitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExhibitorApplication.class, args);
    }
}
```

- [ ] **Step 4: Add runtime configuration**

Create `backend/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: exhibitor-system
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/exhibitor}
    username: ${SPRING_DATASOURCE_USERNAME:exhibitor}
    password: ${SPRING_DATASOURCE_PASSWORD:exhibitor}
    hikari:
      maximum-pool-size: ${SPRING_DATASOURCE_MAX_POOL_SIZE:8}
      minimum-idle: ${SPRING_DATASOURCE_MIN_IDLE:2}
  jpa:
    hibernate:
      ddl-auto: none
    open-in-view: false
    properties:
      hibernate:
        default_schema: ${app.db.schema}
  data:
    redis:
      host: ${SPRING_REDIS_HOST:localhost}
      port: ${SPRING_REDIS_PORT:6379}
      timeout: ${SPRING_REDIS_TIMEOUT:2s}
app:
  db:
    schema: ${APP_DB_SCHEMA:exhibitor_sys}
server:
  port: ${SERVER_PORT:8080}
```

- [ ] **Step 5: Run test**

Run:

```bash
cd backend
mvn test -Dtest=FoundationApplicationTest
```

Expected: PASS after `ExhibitorApplication` is created. Before Step 3 it fails only because the application class does not exist; this test must not require PostgreSQL or Redis.

- [ ] **Step 6: Commit**

```bash
git add backend
git commit -m "chore: create spring boot backend foundation"
```

## Task 2: Add PostgreSQL Dedicated Schema Initializer

**Files:**
- Create: `backend/src/main/java/com/sportexpo/exhibitor/config/SchemaInitializer.java`
- Create: `backend/src/main/resources/db/schema-foundation.sql`
- Modify: `backend/src/test/java/com/sportexpo/exhibitor/FoundationApplicationTest.java`

- [ ] **Step 1: Add Testcontainers-backed context test**

Replace `backend/src/test/java/com/sportexpo/exhibitor/FoundationApplicationTest.java`:

```java
package com.sportexpo.exhibitor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class FoundationApplicationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("exhibitor")
            .withUsername("exhibitor")
            .withPassword("exhibitor");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.db.schema", () -> "exhibitor_sys_test");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "exhibitor_sys_test");
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void createsFoundationTablesInsideDedicatedSchema() {
        var tableCount = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = 'exhibitor_sys_test'
                  and table_name in ('events', 'booths', 'contract_versions', 'refunds', 'booth_release_tasks')
                """, Integer.class);

        assertThat(tableCount).isEqualTo(5);
    }
}
```

- [ ] **Step 2: Add schema initializer**

Create `backend/src/main/java/com/sportexpo/exhibitor/config/SchemaInitializer.java`:

```java
package com.sportexpo.exhibitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import java.nio.charset.StandardCharsets;

@Component
public class SchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final String schema;
    private final Environment environment;

    public SchemaInitializer(JdbcTemplate jdbcTemplate, @Value("${app.db.schema}") String schema,
                             Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.schema = schema;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!schema.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("Invalid schema name: " + schema);
        }
        var hibernateSchema = environment.getProperty("spring.jpa.properties.hibernate.default_schema");
        if (hibernateSchema != null && !hibernateSchema.equals(schema)) {
            throw new IllegalStateException("Hibernate default schema must equal app.db.schema");
        }
        jdbcTemplate.execute("create schema if not exists " + schema);
        var resource = new ClassPathResource("db/schema-foundation.sql");
        var sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        var schemaQualifiedSql = sql.replace("__schema__", schema);
        var scriptResource = new ByteArrayResource(schemaQualifiedSql.getBytes(StandardCharsets.UTF_8));
        jdbcTemplate.execute(connection -> {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(scriptResource, StandardCharsets.UTF_8));
            return null;
        });
    }
}
```

- [ ] **Step 3: Add schema SQL**

Create `backend/src/main/resources/db/schema-foundation.sql`:

```sql
create table if not exists __schema__.events (
    id uuid primary key,
    name varchar(200) not null,
    year int not null,
    status varchar(40) not null,
    registration_start_at timestamptz,
    registration_end_at timestamptz,
    payment_deadline_at timestamptz,
    selection_start_at timestamptz,
    selection_end_at timestamptz,
    service_start_at timestamptz,
    service_end_at timestamptz,
    default_lock_minutes int not null default 1440,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.pavilions (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    name varchar(200) not null,
    code varchar(80) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid,
    unique(event_id, code)
);

create table if not exists __schema__.zones (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    pavilion_id uuid not null references __schema__.pavilions(id),
    name varchar(200) not null,
    code varchar(80) not null,
    category_code varchar(80) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid,
    unique(event_id, code)
);

create table if not exists __schema__.booths (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    pavilion_id uuid references __schema__.pavilions(id),
    zone_id uuid references __schema__.zones(id),
    booth_no varchar(80) not null,
    area_sqm numeric(14,2) not null,
    booth_type varchar(60) not null,
    status varchar(40) not null,
    x numeric(14,2),
    y numeric(14,2),
    width numeric(14,2),
    height numeric(14,2),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid,
    unique(event_id, booth_no)
);

create table if not exists __schema__.area_tiers (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    zone_id uuid references __schema__.zones(id),
    area_sqm numeric(14,2) not null,
    enabled boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.price_rules (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    zone_id uuid references __schema__.zones(id),
    booth_type varchar(60) not null,
    unit_price numeric(14,2) not null,
    enabled boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.contracts (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    current_version_id uuid,
    contract_no varchar(100) not null,
    status varchar(40) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid,
    unique(event_id, contract_no)
);

create table if not exists __schema__.contract_versions (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    contract_id uuid not null references __schema__.contracts(id),
    version_no int not null,
    reason varchar(200) not null,
    content_snapshot jsonb not null,
    amount numeric(14,2) not null,
    area_sqm numeric(14,2) not null,
    sign_status varchar(40) not null,
    signer_id uuid,
    signed_at timestamptz,
    file_id uuid,
    created_at timestamptz not null,
    created_by uuid,
    unique(contract_id, version_no)
);

create table if not exists __schema__.participation_orders (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    exhibitor_id uuid not null,
    registration_id uuid,
    zone_id uuid references __schema__.zones(id),
    area_tier_id uuid references __schema__.area_tiers(id),
    status varchar(40) not null,
    area_fee numeric(14,2) not null,
    service_fee numeric(14,2) not null default 0,
    discount_amount numeric(14,2) not null default 0,
    adjustment_amount numeric(14,2) not null default 0,
    total_amount numeric(14,2) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.payments (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    order_id uuid not null references __schema__.participation_orders(id),
    amount numeric(14,2) not null,
    status varchar(40) not null,
    method varchar(40) not null,
    confirmed_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.refunds (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    order_id uuid not null references __schema__.participation_orders(id),
    amount numeric(14,2) not null,
    status varchar(40) not null,
    reason varchar(500) not null,
    approved_by uuid,
    processed_by uuid,
    processed_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.refund_payment_items (
    id uuid primary key,
    refund_id uuid not null references __schema__.refunds(id),
    payment_id uuid not null references __schema__.payments(id),
    amount numeric(14,2) not null,
    created_at timestamptz not null,
    created_by uuid
);

create table if not exists __schema__.selection_eligibilities (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    order_id uuid not null references __schema__.participation_orders(id),
    status varchar(40) not null,
    pause_reason varchar(80),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid,
    unique(order_id)
);

create table if not exists __schema__.booth_selections (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    order_id uuid not null references __schema__.participation_orders(id),
    exhibitor_id uuid not null,
    status varchar(40) not null,
    total_area_sqm numeric(14,2) not null,
    lock_started_at timestamptz,
    lock_expires_at timestamptz,
    helper_user_id uuid,
    reviewer_user_id uuid,
    reviewed_at timestamptz,
    review_comment varchar(500),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.booth_locks (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    order_id uuid not null references __schema__.participation_orders(id),
    selection_id uuid not null references __schema__.booth_selections(id),
    booth_id uuid not null references __schema__.booths(id),
    status varchar(40) not null,
    lock_expires_at timestamptz not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.booth_release_tasks (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    order_id uuid not null references __schema__.participation_orders(id),
    selection_id uuid references __schema__.booth_selections(id),
    status varchar(40) not null,
    reason varchar(500) not null,
    handled_by uuid,
    handled_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.audit_logs (
    id uuid primary key,
    event_id uuid,
    actor_id uuid,
    action varchar(100) not null,
    object_type varchar(100) not null,
    object_id uuid,
    old_status varchar(40),
    new_status varchar(40),
    reason varchar(500),
    request_source varchar(80),
    created_at timestamptz not null
);

create index if not exists idx_booths_event_status on __schema__.booths(event_id, status);
create index if not exists idx_payments_order_status on __schema__.payments(order_id, status);
create index if not exists idx_refunds_order_status on __schema__.refunds(order_id, status);
create index if not exists idx_booth_locks_event_booth_status on __schema__.booth_locks(event_id, booth_id, status);
create index if not exists idx_release_tasks_event_status on __schema__.booth_release_tasks(event_id, status);
create index if not exists idx_audit_logs_object on __schema__.audit_logs(object_type, object_id);
```

- [ ] **Step 4: Run application context test**

Run:

```bash
cd backend
mvn test -Dtest=FoundationApplicationTest
```

Expected: PASS, with `SchemaInitializer` creating schema `exhibitor_sys_test` and all foundation tables inside that schema.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/sportexpo/exhibitor/config/SchemaInitializer.java backend/src/main/resources/db/schema-foundation.sql backend/src/test/java/com/sportexpo/exhibitor/FoundationApplicationTest.java
git commit -m "feat: add dedicated schema initializer"
```

## Task 3: Add Shared Backend Response and Audit Utilities

**Files:**
- Create: `backend/src/main/java/com/sportexpo/exhibitor/common/ApiResponse.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/common/PageResponse.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/common/BusinessException.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/audit/AuditLog.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/audit/AuditLogRepository.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/audit/AuditService.java`

- [ ] **Step 1: Create common response wrapper**

Create `backend/src/main/java/com/sportexpo/exhibitor/common/ApiResponse.java`:

```java
package com.sportexpo.exhibitor.common;

public record ApiResponse<T>(boolean success, T data, String message) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "OK");
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
```

- [ ] **Step 2: Create page response**

Create `backend/src/main/java/com/sportexpo/exhibitor/common/PageResponse.java`:

```java
package com.sportexpo.exhibitor.common;

import java.util.List;

public record PageResponse<T>(List<T> records, long total, int page, int pageSize) {
}
```

- [ ] **Step 3: Create business exception**

Create `backend/src/main/java/com/sportexpo/exhibitor/common/BusinessException.java`:

```java
package com.sportexpo.exhibitor.common;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
```

- [ ] **Step 4: Create audit entity and repository**

Create `backend/src/main/java/com/sportexpo/exhibitor/audit/AuditLog.java`:

```java
package com.sportexpo.exhibitor.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "event_id")
    private UUID eventId;
    @Column(name = "actor_id")
    private UUID actorId;
    @Column(name = "action")
    private String action;
    @Column(name = "object_type")
    private String objectType;
    @Column(name = "object_id")
    private UUID objectId;
    @Column(name = "old_status")
    private String oldStatus;
    @Column(name = "new_status")
    private String newStatus;
    @Column(name = "reason")
    private String reason;
    @Column(name = "request_source")
    private String requestSource;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected AuditLog() {
    }

    public AuditLog(UUID id, UUID eventId, UUID actorId, String action, String objectType, UUID objectId,
                    String oldStatus, String newStatus, String reason, String requestSource, OffsetDateTime createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.actorId = actorId;
        this.action = action;
        this.objectType = objectType;
        this.objectId = objectId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.requestSource = requestSource;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getActorId() { return actorId; }
    public String getAction() { return action; }
    public String getObjectType() { return objectType; }
    public UUID getObjectId() { return objectId; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public String getReason() { return reason; }
    public String getRequestSource() { return requestSource; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
```

Create `backend/src/main/java/com/sportexpo/exhibitor/audit/AuditLogRepository.java`:

```java
package com.sportexpo.exhibitor.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
```

- [ ] **Step 5: Create audit service**

Create `backend/src/main/java/com/sportexpo/exhibitor/audit/AuditService.java`:

```java
package com.sportexpo.exhibitor.audit;

import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuditService {
    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(UUID eventId, UUID actorId, String action, String objectType, UUID objectId,
                       String oldStatus, String newStatus, String reason) {
        repository.save(new AuditLog(
                UUID.randomUUID(),
                eventId,
                actorId,
                action,
                objectType,
                objectId,
                oldStatus,
                newStatus,
                reason,
                "api",
                OffsetDateTime.now()
        ));
    }
}
```

- [ ] **Step 6: Run tests**

Run:

```bash
cd backend
mvn test -Dtest=FoundationApplicationTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/sportexpo/exhibitor/common backend/src/main/java/com/sportexpo/exhibitor/audit
git commit -m "feat: add common responses and audit service"
```

## Task 4: Implement Event Master Data API

**Files:**
- Create: `backend/src/main/java/com/sportexpo/exhibitor/event/Event.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/event/EventRepository.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/event/dto/CreateEventRequest.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/event/dto/EventResponse.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/event/EventService.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/event/EventController.java`
- Create: `backend/src/test/java/com/sportexpo/exhibitor/event/EventServiceTest.java`

- [ ] **Step 1: Write failing service test**

Create `backend/src/test/java/com/sportexpo/exhibitor/event/EventServiceTest.java`:

```java
package com.sportexpo.exhibitor.event;

import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class EventServiceTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("exhibitor")
            .withUsername("exhibitor")
            .withPassword("exhibitor");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.db.schema", () -> "exhibitor_sys_test");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "exhibitor_sys_test");
    }

    @Autowired
    EventService eventService;

    @Test
    void createsEvent() {
        var response = eventService.create(new CreateEventRequest("中国国际体育用品博览会", 2026));

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("中国国际体育用品博览会");
        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.status()).isEqualTo("DRAFT");
    }
}
```

- [ ] **Step 2: Add DTOs**

Create `backend/src/main/java/com/sportexpo/exhibitor/event/dto/CreateEventRequest.java`:

```java
package com.sportexpo.exhibitor.event.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateEventRequest(
        @NotBlank String name,
        @Min(2000) int year
) {
}
```

Create `backend/src/main/java/com/sportexpo/exhibitor/event/dto/EventResponse.java`:

```java
package com.sportexpo.exhibitor.event.dto;

import java.util.UUID;

public record EventResponse(UUID id, String name, int year, String status) {
}
```

- [ ] **Step 3: Add entity and repository**

Create `backend/src/main/java/com/sportexpo/exhibitor/event/Event.java`:

```java
package com.sportexpo.exhibitor.event;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "name")
    private String name;
    @Column(name = "year")
    private int year;
    @Column(name = "status")
    private String status;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected Event() {
    }

    public Event(UUID id, String name, int year, String status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.year = year;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public int getYear() { return year; }
    public String getStatus() { return status; }
}
```

Create `backend/src/main/java/com/sportexpo/exhibitor/event/EventRepository.java`:

```java
package com.sportexpo.exhibitor.event;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
}
```

- [ ] **Step 4: Add service and controller**

Create `backend/src/main/java/com/sportexpo/exhibitor/event/EventService.java`:

```java
package com.sportexpo.exhibitor.event;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.event.dto.EventResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EventService {
    private final EventRepository repository;
    private final AuditService auditService;

    public EventService(EventRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional
    public EventResponse create(CreateEventRequest request) {
        var now = OffsetDateTime.now();
        var event = repository.save(new Event(UUID.randomUUID(), request.name(), request.year(), "DRAFT", now, now));
        auditService.record(event.getId(), null, "EVENT_CREATE", "Event", event.getId(), null, "DRAFT", "create event");
        return toResponse(event);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(event.getId(), event.getName(), event.getYear(), event.getStatus());
    }
}
```

Create `backend/src/main/java/com/sportexpo/exhibitor/event/EventController.java`:

```java
package com.sportexpo.exhibitor.event;

import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.event.dto.EventResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @GetMapping
    public ApiResponse<List<EventResponse>> list() {
        return ApiResponse.ok(service.list());
    }
}
```

- [ ] **Step 5: Run test**

Run:

```bash
cd backend
mvn test -Dtest=EventServiceTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/sportexpo/exhibitor/event backend/src/test/java/com/sportexpo/exhibitor/event/EventServiceTest.java
git commit -m "feat: add event master data api"
```

## Task 5: Implement Booth Master Data API

**Files:**
- Create: `backend/src/main/java/com/sportexpo/exhibitor/booth/Booth.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/booth/BoothRepository.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/booth/dto/CreateBoothRequest.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/booth/dto/BoothResponse.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/booth/BoothService.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/booth/BoothController.java`
- Create: `backend/src/test/java/com/sportexpo/exhibitor/booth/BoothServiceTest.java`

- [ ] **Step 1: Write failing service test**

Create `backend/src/test/java/com/sportexpo/exhibitor/booth/BoothServiceTest.java`:

```java
package com.sportexpo.exhibitor.booth;

import com.sportexpo.exhibitor.booth.dto.CreateBoothRequest;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class BoothServiceTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("exhibitor")
            .withUsername("exhibitor")
            .withPassword("exhibitor");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.db.schema", () -> "exhibitor_sys_test");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "exhibitor_sys_test");
    }

    @Autowired
    EventService eventService;

    @Autowired
    BoothService boothService;

    @Test
    void createsBoothInEvent() {
        var event = eventService.create(new CreateEventRequest("中国国际体育用品博览会", 2026));

        var booth = boothService.create(new CreateBoothRequest(
                event.id(),
                "A001",
                new BigDecimal("9.00"),
                "STANDARD"
        ));

        assertThat(booth.eventId()).isEqualTo(event.id());
        assertThat(booth.boothNo()).isEqualTo("A001");
        assertThat(booth.areaSqm()).isEqualByComparingTo("9.00");
        assertThat(booth.status()).isEqualTo("AVAILABLE");
    }
}
```

- [ ] **Step 2: Add DTOs**

Create `backend/src/main/java/com/sportexpo/exhibitor/booth/dto/CreateBoothRequest.java`:

```java
package com.sportexpo.exhibitor.booth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateBoothRequest(
        @NotNull UUID eventId,
        @NotBlank String boothNo,
        @NotNull @Positive BigDecimal areaSqm,
        @NotBlank String boothType
) {
}
```

Create `backend/src/main/java/com/sportexpo/exhibitor/booth/dto/BoothResponse.java`:

```java
package com.sportexpo.exhibitor.booth.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BoothResponse(UUID id, UUID eventId, String boothNo, BigDecimal areaSqm, String boothType, String status) {
}
```

- [ ] **Step 3: Add entity and repository**

Create `backend/src/main/java/com/sportexpo/exhibitor/booth/Booth.java`:

```java
package com.sportexpo.exhibitor.booth;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booths")
public class Booth {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "event_id")
    private UUID eventId;
    @Column(name = "booth_no")
    private String boothNo;
    @Column(name = "area_sqm")
    private BigDecimal areaSqm;
    @Column(name = "booth_type")
    private String boothType;
    @Column(name = "status")
    private String status;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected Booth() {
    }

    public Booth(UUID id, UUID eventId, String boothNo, BigDecimal areaSqm, String boothType,
                 String status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.eventId = eventId;
        this.boothNo = boothNo;
        this.areaSqm = areaSqm;
        this.boothType = boothType;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getBoothNo() { return boothNo; }
    public BigDecimal getAreaSqm() { return areaSqm; }
    public String getBoothType() { return boothType; }
    public String getStatus() { return status; }
}
```

Create `backend/src/main/java/com/sportexpo/exhibitor/booth/BoothRepository.java`:

```java
package com.sportexpo.exhibitor.booth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BoothRepository extends JpaRepository<Booth, UUID> {
    List<Booth> findByEventId(UUID eventId);
}
```

- [ ] **Step 4: Add service and controller**

Create `backend/src/main/java/com/sportexpo/exhibitor/booth/BoothService.java`:

```java
package com.sportexpo.exhibitor.booth;

import com.sportexpo.exhibitor.audit.AuditService;
import com.sportexpo.exhibitor.booth.dto.BoothResponse;
import com.sportexpo.exhibitor.booth.dto.CreateBoothRequest;
import com.sportexpo.exhibitor.common.BusinessException;
import com.sportexpo.exhibitor.event.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BoothService {
    private final BoothRepository repository;
    private final EventRepository eventRepository;
    private final AuditService auditService;

    public BoothService(BoothRepository repository, EventRepository eventRepository, AuditService auditService) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.auditService = auditService;
    }

    @Transactional
    public BoothResponse create(CreateBoothRequest request) {
        eventRepository.findById(request.eventId())
                .orElseThrow(() -> new BusinessException("Event does not exist"));
        var now = OffsetDateTime.now();
        var booth = repository.save(new Booth(
                UUID.randomUUID(),
                request.eventId(),
                request.boothNo(),
                request.areaSqm(),
                request.boothType(),
                "AVAILABLE",
                now,
                now
        ));
        auditService.record(booth.getEventId(), null, "BOOTH_CREATE", "Booth", booth.getId(), null, "AVAILABLE", "create booth");
        return toResponse(booth);
    }

    @Transactional(readOnly = true)
    public List<BoothResponse> list(UUID eventId) {
        return repository.findByEventId(eventId).stream().map(this::toResponse).toList();
    }

    private BoothResponse toResponse(Booth booth) {
        return new BoothResponse(
                booth.getId(),
                booth.getEventId(),
                booth.getBoothNo(),
                booth.getAreaSqm(),
                booth.getBoothType(),
                booth.getStatus()
        );
    }
}
```

Create `backend/src/main/java/com/sportexpo/exhibitor/booth/BoothController.java`:

```java
package com.sportexpo.exhibitor.booth;

import com.sportexpo.exhibitor.booth.dto.BoothResponse;
import com.sportexpo.exhibitor.booth.dto.CreateBoothRequest;
import com.sportexpo.exhibitor.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/booths")
public class BoothController {
    private final BoothService service;

    public BoothController(BoothService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<BoothResponse> create(@Valid @RequestBody CreateBoothRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @GetMapping
    public ApiResponse<List<BoothResponse>> list(@RequestParam UUID eventId) {
        return ApiResponse.ok(service.list(eventId));
    }
}
```

- [ ] **Step 5: Run tests**

Run:

```bash
cd backend
mvn test -Dtest=BoothServiceTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/sportexpo/exhibitor/booth backend/src/test/java/com/sportexpo/exhibitor/booth/BoothServiceTest.java
git commit -m "feat: add booth master data api"
```

## Task 6: Create Frontend Project with Ant Design

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/index.html`
- Create: `frontend/tsconfig.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/src/main.tsx`
- Create: `frontend/src/app/App.tsx`
- Create: `frontend/src/app/routes.tsx`
- Create: `frontend/src/app/request.ts`
- Create: `frontend/src/app/layouts/AdminLayout.tsx`
- Create: `frontend/src/app/layouts/ExhibitorLayout.tsx`
- Create: `frontend/src/pages/LoginPage.tsx`
- Create: `frontend/src/pages/admin/DashboardPage.tsx`
- Create: `frontend/src/pages/admin/events/EventListPage.tsx`
- Create: `frontend/src/pages/admin/booths/BoothListPage.tsx`
- Create: `frontend/src/pages/exhibitor/HomePage.tsx`
- Create: `frontend/src/styles/global.css`

- [ ] **Step 1: Add package manifest**

Create `frontend/package.json`:

```json
{
  "name": "exhibitor-system-frontend",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "test": "vitest run"
  },
  "dependencies": {
    "@ant-design/icons": "^5.5.1",
    "@ant-design/pro-components": "^2.8.1",
    "antd": "^5.21.6",
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "react-router-dom": "^6.27.0"
  },
  "devDependencies": {
    "@types/react": "^18.3.11",
    "@types/react-dom": "^18.3.1",
    "@vitejs/plugin-react": "^4.3.3",
    "typescript": "^5.6.3",
    "vite": "^5.4.10",
    "vitest": "^2.1.3"
  }
}
```

- [ ] **Step 2: Add Vite/TypeScript files**

Create `frontend/index.html`:

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>展商系统</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

Create `frontend/tsconfig.json`:

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["DOM", "DOM.Iterable", "ES2020"],
    "allowJs": false,
    "skipLibCheck": true,
    "esModuleInterop": true,
    "allowSyntheticDefaultImports": true,
    "strict": true,
    "forceConsistentCasingInFileNames": true,
    "module": "ESNext",
    "moduleResolution": "Node",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "types": ["vitest/globals"]
  },
  "include": ["src"],
  "references": []
}
```

Create `frontend/vite.config.ts`:

```ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
```

- [ ] **Step 3: Add app shell and routes**

Create `frontend/src/main.tsx`:

```tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { ConfigProvider, App as AntdApp } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import App from './app/App';
import './styles/global.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN}>
      <AntdApp>
        <App />
      </AntdApp>
    </ConfigProvider>
  </React.StrictMode>,
);
```

Create `frontend/src/app/App.tsx`:

```tsx
import { RouterProvider, createBrowserRouter } from 'react-router-dom';
import { routes } from './routes';

const router = createBrowserRouter(routes);

export default function App() {
  return <RouterProvider router={router} />;
}
```

Create `frontend/src/app/routes.tsx`:

```tsx
import { lazy, Suspense } from 'react';
import type { RouteObject } from 'react-router-dom';
import { Spin } from 'antd';
import AdminLayout from './layouts/AdminLayout';
import ExhibitorLayout from './layouts/ExhibitorLayout';

const LoginPage = lazy(() => import('../pages/LoginPage'));
const DashboardPage = lazy(() => import('../pages/admin/DashboardPage'));
const EventListPage = lazy(() => import('../pages/admin/events/EventListPage'));
const BoothListPage = lazy(() => import('../pages/admin/booths/BoothListPage'));
const ExhibitorHomePage = lazy(() => import('../pages/exhibitor/HomePage'));

function lazyPage(node: JSX.Element) {
  return <Suspense fallback={<Spin fullscreen />}>{node}</Suspense>;
}

export const routes: RouteObject[] = [
  { path: '/', element: lazyPage(<LoginPage />) },
  {
    path: '/admin',
    element: <AdminLayout />,
    children: [
      { index: true, element: lazyPage(<DashboardPage />) },
      { path: 'events', element: lazyPage(<EventListPage />) },
      { path: 'booths', element: lazyPage(<BoothListPage />) },
    ],
  },
  {
    path: '/exhibitor',
    element: <ExhibitorLayout />,
    children: [
      { index: true, element: lazyPage(<ExhibitorHomePage />) },
    ],
  },
];
```

- [ ] **Step 4: Add API request helper**

Create `frontend/src/app/request.ts`:

```ts
import { message } from 'antd';

type ApiResponse<T> = {
  success: boolean;
  data: T;
  message?: string;
};

export async function apiGet<T>(url: string): Promise<T> {
  const response = await fetch(url, {
    headers: {
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    message.error('请求失败，请稍后重试');
    throw new Error(`Request failed: ${response.status}`);
  }

  const body = (await response.json()) as ApiResponse<T>;
  if (!body.success) {
    message.error(body.message || '请求失败，请稍后重试');
    throw new Error(body.message || 'Request failed');
  }

  return body.data;
}
```

- [ ] **Step 5: Add Ant Design layouts**

Create `frontend/src/app/layouts/AdminLayout.tsx`:

```tsx
import { ProLayout } from '@ant-design/pro-components';
import { Outlet, useNavigate } from 'react-router-dom';

export default function AdminLayout() {
  const navigate = useNavigate();
  return (
    <ProLayout
      title="展商系统后台"
      layout="mix"
      route={{
        path: '/admin',
        routes: [
          { path: '/admin', name: '工作台' },
          { path: '/admin/events', name: '展会管理' },
          { path: '/admin/booths', name: '展位管理' },
        ],
      }}
      menuItemRender={(item, dom) => (
        <a onClick={() => item.path && navigate(item.path)}>{dom}</a>
      )}
    >
      <Outlet />
    </ProLayout>
  );
}
```

Create `frontend/src/app/layouts/ExhibitorLayout.tsx`:

```tsx
import { Layout, Menu } from 'antd';
import { Outlet, useNavigate } from 'react-router-dom';

export default function ExhibitorLayout() {
  const navigate = useNavigate();
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Header>
        <Menu
          theme="dark"
          mode="horizontal"
          selectedKeys={['home']}
          items={[{ key: 'home', label: '展商中心' }]}
          onClick={() => navigate('/exhibitor')}
        />
      </Layout.Header>
      <Layout.Content style={{ padding: 24 }}>
        <Outlet />
      </Layout.Content>
    </Layout>
  );
}
```

- [ ] **Step 6: Add pages**

Create `frontend/src/pages/LoginPage.tsx`:

```tsx
import { Button, Card, Space, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';

export default function LoginPage() {
  const navigate = useNavigate();
  return (
    <div className="login-page">
      <Card title="中国国际体育用品博览会展商系统" style={{ width: 420 }}>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Typography.Text type="secondary">请选择入口</Typography.Text>
          <Button type="primary" block onClick={() => navigate('/admin')}>运营后台</Button>
          <Button block onClick={() => navigate('/exhibitor')}>展商中心</Button>
        </Space>
      </Card>
    </div>
  );
}
```

Create `frontend/src/pages/admin/DashboardPage.tsx`:

```tsx
import { PageContainer, ProCard } from '@ant-design/pro-components';
import { Col, Row, Statistic } from 'antd';

export default function DashboardPage() {
  return (
    <PageContainer title="工作台">
      <Row gutter={16}>
        <Col span={6}><ProCard><Statistic title="待审核报名" value={0} /></ProCard></Col>
        <Col span={6}><ProCard><Statistic title="待销售确认订单" value={0} /></ProCard></Col>
        <Col span={6}><ProCard><Statistic title="待审核选位" value={0} /></ProCard></Col>
        <Col span={6}><ProCard><Statistic title="待处理释放待办" value={0} /></ProCard></Col>
      </Row>
    </PageContainer>
  );
}
```

Create `frontend/src/pages/admin/events/EventListPage.tsx`:

```tsx
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { apiGet } from '../../../app/request';

type EventRow = {
  id: string;
  name: string;
  year: number;
  status: string;
};

export default function EventListPage() {
  return (
    <PageContainer title="展会管理">
      <ProTable
        rowKey="id"
        search={false}
        columns={[
          { title: '展会名称', dataIndex: 'name' },
          { title: '年份', dataIndex: 'year' },
          { title: '状态', dataIndex: 'status' },
        ]}
        request={async () => {
          const data = await apiGet<EventRow[]>('/api/events');
          return { data, success: true, total: data.length };
        }}
      />
    </PageContainer>
  );
}
```

Create `frontend/src/pages/admin/booths/BoothListPage.tsx`:

```tsx
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { apiGet } from '../../../app/request';

type BoothRow = {
  id: string;
  eventId: string;
  boothNo: string;
  areaSqm: number;
  boothType: string;
  status: string;
};

export default function BoothListPage() {
  return (
    <PageContainer title="展位管理">
      <ProTable
        rowKey="id"
        locale={{ emptyText: '请选择展会后查看展位' }}
        search={{ labelWidth: 'auto' }}
        columns={[
          { title: '展会 ID', dataIndex: 'eventId', hideInTable: true },
          { title: '展位号', dataIndex: 'boothNo' },
          { title: '面积', dataIndex: 'areaSqm', valueType: 'digit' },
          { title: '类型', dataIndex: 'boothType' },
          { title: '状态', dataIndex: 'status' },
        ]}
        request={async (params) => {
          if (!params.eventId) {
            return { data: [], success: true, total: 0 };
          }

          const eventId = encodeURIComponent(String(params.eventId));
          const data = await apiGet<BoothRow[]>(`/api/booths?eventId=${eventId}`);
          return { data, success: true, total: data.length };
        }}
      />
    </PageContainer>
  );
}
```

Foundation 阶段 `BoothListPage` 通过手工输入展会 ID 查询展位，用于验证前后端 API 链路。后续业务计划接入当前展会切换上下文或 Event 下拉选择器后，再将展会 ID 输入替换为标准选择控件。

Create `frontend/src/pages/exhibitor/HomePage.tsx`:

```tsx
import { Card, Steps } from 'antd';

export default function HomePage() {
  return (
    <Card title="参展进度">
      <Steps
        current={0}
        items={[
          { title: '报名' },
          { title: '选平米数' },
          { title: '签合同' },
          { title: '付款' },
          { title: '选位置' },
          { title: '服务办理' },
        ]}
      />
    </Card>
  );
}
```

Create `frontend/src/styles/global.css`:

```css
html,
body,
#root {
  min-height: 100%;
  margin: 0;
}

.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}
```

- [ ] **Step 7: Run build**

Run:

```bash
cd frontend
npm install
npm run build
```

Expected: TypeScript and Vite build pass.

- [ ] **Step 8: Commit**

```bash
git add frontend
git commit -m "feat: add react ant design frontend foundation"
```

## Task 7: Add Local Development Compose and README

**Files:**
- Create: `docker-compose.yml`
- Create: `README.md`

- [ ] **Step 1: Add local infrastructure**

Create `docker-compose.yml`:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: exhibitor
      POSTGRES_USER: exhibitor
      POSTGRES_PASSWORD: exhibitor
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

volumes:
  postgres_data:
```

- [ ] **Step 2: Add README**

Create `README.md`:

````markdown
# 中国国际体育用品博览会展商系统

## 技术栈

- Frontend: React + TypeScript + Ant Design + ProComponents
- Backend: Java 17 + Spring Boot 3
- Database: PostgreSQL
- Cache/TTL: Redis

## Local Development

Start infrastructure:

```bash
docker compose up -d
```

Run backend:

```bash
cd backend
mvn spring-boot:run
```

The backend creates and uses the schema configured by `APP_DB_SCHEMA`.

For a shared PostgreSQL database, provide:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/database
SPRING_DATASOURCE_USERNAME=username
SPRING_DATASOURCE_PASSWORD=password
APP_DB_SCHEMA=exhibitor_sys
```

Run frontend:

```bash
cd frontend
npm install
npm run dev
```

Frontend dev server: http://localhost:5173

Backend API: http://localhost:8080

## Verification

Backend tests:

```bash
cd backend
mvn test
```

Frontend build:

```bash
cd frontend
npm run build
```
````

- [ ] **Step 3: Run smoke checks**

Run:

```bash
docker compose config
```

Expected: compose file validates.

Run:

```bash
cd backend
mvn test
```

Expected: all backend tests pass.

Run:

```bash
cd frontend
npm run build
```

Expected: frontend build passes.

- [ ] **Step 4: Commit**

```bash
git add docker-compose.yml README.md
git commit -m "chore: add local development docs"
```

## Self-Review Checklist

- [ ] Backend uses Spring Boot 3, Java 17, PostgreSQL, dedicated schema initializer, Redis dependency.
- [ ] Schema initializer uses `ScriptUtils`, does not split SQL by semicolon, and does not rely on connection-pool session `search_path`.
- [ ] `spring.jpa.properties.hibernate.default_schema` references `${app.db.schema}`, and `SchemaInitializer` validates both resolved schema values are equal.
- [ ] Default schema is `exhibitor_sys`; test schema is `exhibitor_sys_test`; no legacy schema naming remains.
- [ ] Schema initializer creates contract_versions, refunds, refund_payment_items, booth_release_tasks, selection_eligibilities inside the configured schema.
- [ ] Foundation stage implements Java Repository/Service only for Event and Booth; contract/refund/release/selection tables are DDL-only placeholders for later plans.
- [ ] Money columns use numeric(14,2).
- [ ] Audit fields include event_id, actor_id, object_type, object_id, old_status, new_status, reason, request_source, and created_at.
- [ ] Frontend uses React, TypeScript, Ant Design, ProComponents.
- [ ] Admin and exhibitor routes are lazy-loaded.
- [ ] Frontend ProTable pages call backend APIs through `frontend/src/app/request.ts`.
- [ ] Event and booth APIs write audit logs.
- [ ] Tests prove event and booth creation.
- [ ] README gives exact local run commands.
- [ ] Document constraints reconcile with technical design: schema isolation, no migration-tool dependency, no historical migration, numeric precision, and strict Ant Design component usage.

## Foundation Deferred Items

- [ ] `schema-foundation.sql` is the v1 full baseline. Later implementation plans must add incremental `db/schema-v{N}.sql` scripts plus a `schema_version` table or equivalent mechanism, and extend `SchemaInitializer` to execute pending scripts by version after approval.
- [ ] `BoothListPage` currently verifies the backend list API through a manual event ID filter. Later UI plans must replace this with current-event context or an Ant Design selector populated from `/api/events`.
- [ ] Foundation entities intentionally map only the columns needed by the current Event/Booth APIs. The next selection/order plan must complete mappings for event time windows, `default_lock_minutes`, booth pavilion/zone IDs, and booth coordinate fields before implementing lock TTL and map selection.
- [ ] `AuditService` currently records `request_source` as `api`. Later audit work must resolve request source from HTTP context, scheduled jobs, and future async/MQ entry points.
- [ ] `BoothListPage` should not require users to paste an event UUID in production. Replace the manual filter with current-event context or an Ant Design event selector populated from `/api/events`.
