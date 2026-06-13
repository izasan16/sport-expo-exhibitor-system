# Order Contract Payment Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the second-round exhibitor business loop from area tier and price maintenance through exhibitor order, sales confirmation, contract marking, payment confirmation, booth selection, review approval, rejection, and timeout release.

**Architecture:** Extend the existing Spring Boot modular monolith with focused `pricing`, `order`, `contract`, `payment`, and `selection` modules. PostgreSQL remains the source of truth; row-level locks provide the concurrency boundary for payment aggregation and booth selection. The React frontend continues as one Vite application with lazy admin and exhibitor route groups, using Ant Design and ProComponents only for tables, forms, drawers, and actions.

**Tech Stack:** Java 17, Spring Boot 3, Spring Data JPA, PostgreSQL, JUnit 5, MockMvc, React, TypeScript, Vite, Ant Design, ProComponents.

---

## File Structure

Modify the current worktree at `D:\project\pe\.worktrees\foundation-implementation`.

Backend files to create:

```text
backend/src/main/java/com/sportexpo/exhibitor/common/DemoActors.java
backend/src/main/java/com/sportexpo/exhibitor/common/BusinessStatuses.java
backend/src/main/java/com/sportexpo/exhibitor/pricing/AreaTier.java
backend/src/main/java/com/sportexpo/exhibitor/pricing/AreaTierRepository.java
backend/src/main/java/com/sportexpo/exhibitor/pricing/AreaTierService.java
backend/src/main/java/com/sportexpo/exhibitor/pricing/AreaTierController.java
backend/src/main/java/com/sportexpo/exhibitor/pricing/PriceRule.java
backend/src/main/java/com/sportexpo/exhibitor/pricing/PriceRuleRepository.java
backend/src/main/java/com/sportexpo/exhibitor/pricing/PriceRuleService.java
backend/src/main/java/com/sportexpo/exhibitor/pricing/PriceRuleController.java
backend/src/main/java/com/sportexpo/exhibitor/pricing/dto/CreateAreaTierRequest.java
backend/src/main/java/com/sportexpo/exhibitor/pricing/dto/AreaTierResponse.java
backend/src/main/java/com/sportexpo/exhibitor/pricing/dto/CreatePriceRuleRequest.java
backend/src/main/java/com/sportexpo/exhibitor/pricing/dto/PriceRuleResponse.java
backend/src/main/java/com/sportexpo/exhibitor/order/ParticipationOrder.java
backend/src/main/java/com/sportexpo/exhibitor/order/ParticipationOrderRepository.java
backend/src/main/java/com/sportexpo/exhibitor/order/ParticipationOrderService.java
backend/src/main/java/com/sportexpo/exhibitor/order/ParticipationOrderController.java
backend/src/main/java/com/sportexpo/exhibitor/order/dto/CreateParticipationOrderRequest.java
backend/src/main/java/com/sportexpo/exhibitor/order/dto/ParticipationOrderResponse.java
backend/src/main/java/com/sportexpo/exhibitor/contract/Contract.java
backend/src/main/java/com/sportexpo/exhibitor/contract/ContractVersion.java
backend/src/main/java/com/sportexpo/exhibitor/contract/ContractRepository.java
backend/src/main/java/com/sportexpo/exhibitor/contract/ContractVersionRepository.java
backend/src/main/java/com/sportexpo/exhibitor/contract/ContractService.java
backend/src/main/java/com/sportexpo/exhibitor/contract/ContractController.java
backend/src/main/java/com/sportexpo/exhibitor/contract/dto/ContractResponse.java
backend/src/main/java/com/sportexpo/exhibitor/contract/dto/ContractVersionResponse.java
backend/src/main/java/com/sportexpo/exhibitor/payment/Payment.java
backend/src/main/java/com/sportexpo/exhibitor/payment/PaymentRepository.java
backend/src/main/java/com/sportexpo/exhibitor/payment/PaymentService.java
backend/src/main/java/com/sportexpo/exhibitor/payment/PaymentController.java
backend/src/main/java/com/sportexpo/exhibitor/payment/dto/CreatePaymentRequest.java
backend/src/main/java/com/sportexpo/exhibitor/payment/dto/PaymentResponse.java
backend/src/main/java/com/sportexpo/exhibitor/selection/SelectionEligibility.java
backend/src/main/java/com/sportexpo/exhibitor/selection/SelectionEligibilityRepository.java
backend/src/main/java/com/sportexpo/exhibitor/selection/BoothSelection.java
backend/src/main/java/com/sportexpo/exhibitor/selection/BoothSelectionRepository.java
backend/src/main/java/com/sportexpo/exhibitor/selection/BoothLock.java
backend/src/main/java/com/sportexpo/exhibitor/selection/BoothLockRepository.java
backend/src/main/java/com/sportexpo/exhibitor/selection/BoothSelectionService.java
backend/src/main/java/com/sportexpo/exhibitor/selection/BoothSelectionController.java
backend/src/main/java/com/sportexpo/exhibitor/selection/dto/CreateBoothSelectionRequest.java
backend/src/main/java/com/sportexpo/exhibitor/selection/dto/BoothSelectionResponse.java
backend/src/main/java/com/sportexpo/exhibitor/selection/dto/RejectBoothSelectionRequest.java
backend/src/main/java/com/sportexpo/exhibitor/selection/dto/SelectionEligibilityResponse.java
backend/src/test/java/com/sportexpo/exhibitor/pricing/PricingServiceTest.java
backend/src/test/java/com/sportexpo/exhibitor/order/ParticipationOrderServiceTest.java
backend/src/test/java/com/sportexpo/exhibitor/TestBusinessFixture.java
backend/src/test/java/com/sportexpo/exhibitor/contract/ContractFlowTest.java
backend/src/test/java/com/sportexpo/exhibitor/payment/PaymentServiceTest.java
backend/src/test/java/com/sportexpo/exhibitor/selection/BoothSelectionServiceTest.java
```

Backend files to modify:

```text
backend/src/main/resources/db/schema-foundation.sql
backend/src/main/java/com/sportexpo/exhibitor/booth/Booth.java
backend/src/main/java/com/sportexpo/exhibitor/booth/BoothRepository.java
backend/src/main/java/com/sportexpo/exhibitor/booth/BoothService.java
backend/src/main/java/com/sportexpo/exhibitor/booth/BoothController.java
backend/src/main/java/com/sportexpo/exhibitor/booth/dto/BoothResponse.java
backend/src/test/java/com/sportexpo/exhibitor/booth/BoothServiceTest.java
```

Frontend files to create:

```text
frontend/src/app/demoActors.ts
frontend/src/pages/admin/pricing/AreaTierListPage.tsx
frontend/src/pages/admin/pricing/PriceRuleListPage.tsx
frontend/src/pages/admin/orders/AdminOrderListPage.tsx
frontend/src/pages/admin/contracts/ContractListPage.tsx
frontend/src/pages/admin/payments/PaymentConfirmPage.tsx
frontend/src/pages/admin/selections/SelectionReviewPage.tsx
frontend/src/pages/exhibitor/orders/OrderCreatePage.tsx
frontend/src/pages/exhibitor/orders/MyOrdersPage.tsx
frontend/src/pages/exhibitor/payments/PaymentSubmitPage.tsx
frontend/src/pages/exhibitor/selections/BoothSelectionPage.tsx
frontend/src/pages/exhibitor/selections/SelectionResultPage.tsx
```

Frontend files to modify:

```text
frontend/src/app/request.ts
frontend/src/app/routes.tsx
frontend/src/app/layouts/AdminLayout.tsx
frontend/src/app/layouts/ExhibitorLayout.tsx
```

## Shared Conventions

Use these exact status values in Java and TypeScript:

```text
ORDER_PENDING_SALES_CONFIRM = PENDING_SALES_CONFIRM
ORDER_PENDING_CONTRACT = PENDING_CONTRACT
ORDER_PENDING_PAYMENT = PENDING_PAYMENT
ORDER_PARTIALLY_PAID = PARTIALLY_PAID
ORDER_PAID = PAID
ORDER_COMPLETED = COMPLETED

PAYMENT_PENDING_FINANCE_CONFIRM = PENDING_FINANCE_CONFIRM
PAYMENT_CONFIRMED = CONFIRMED
PAYMENT_CONFIRM_FAILED = CONFIRM_FAILED

ELIGIBILITY_NOT_OPEN = NOT_OPEN
ELIGIBILITY_AVAILABLE = AVAILABLE
ELIGIBILITY_PAUSED = PAUSED
ELIGIBILITY_CLOSED = CLOSED
PAUSE_SELECTION_PENDING = selection_pending

SELECTION_PENDING_REVIEW = PENDING_REVIEW
SELECTION_CONFIRMED = CONFIRMED
SELECTION_REJECTED = REJECTED
SELECTION_TIMEOUT = TIMEOUT

BOOTH_AVAILABLE = AVAILABLE
BOOTH_LOCKED = LOCKED
BOOTH_CONFIRMED = CONFIRMED

CONTRACT_GENERATED = GENERATED
CONTRACT_EFFECTIVE = EFFECTIVE
CONTRACT_VERSION_UNSIGNED = UNSIGNED
CONTRACT_VERSION_MARKED_SIGNED = MARKED_SIGNED
```

Use these demo actors in backend and frontend until real authentication lands:

```text
DEMO_EXHIBITOR_ID = 00000000-0000-0000-0000-000000000001
DEMO_ADMIN_ID = 00000000-0000-0000-0000-000000000002
```

## Task 1: Align Schema Baseline and Shared Constants

**Files:**
- Modify: `backend/src/main/resources/db/schema-foundation.sql`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/common/DemoActors.java`
- Create: `backend/src/main/java/com/sportexpo/exhibitor/common/BusinessStatuses.java`
- Modify: `backend/src/test/java/com/sportexpo/exhibitor/FoundationApplicationTest.java`

- [ ] **Step 1: Write failing schema baseline test**

Modify `FoundationApplicationTest` to assert the second-round columns and indexes exist:

```java
@Test
void createsSecondRoundBaselineColumns() {
    var orderColumns = jdbcTemplate.queryForObject("""
            select count(*)
            from information_schema.columns
            where table_schema = ?
              and table_name = 'participation_orders'
              and column_name in ('area_sqm', 'booth_type')
            """, Integer.class, schema);

    var contractOrderColumn = jdbcTemplate.queryForObject("""
            select count(*)
            from information_schema.columns
            where table_schema = ?
              and table_name = 'contracts'
              and column_name = 'order_id'
            """, Integer.class, schema);

    var pricingIndexes = jdbcTemplate.queryForObject("""
            select count(*)
            from pg_indexes
            where schemaname = ?
              and indexname in ('uidx_area_tiers_event_zone_area', 'uidx_price_rules_event_zone_booth_type_enabled')
            """, Integer.class, schema);

    assertThat(orderColumns).isEqualTo(2);
    assertThat(contractOrderColumn).isEqualTo(1);
    assertThat(pricingIndexes).isEqualTo(2);
}
```

Expected before implementation: FAIL because `participation_orders.area_sqm`, `participation_orders.booth_type`, and the pricing uniqueness indexes are missing.

- [ ] **Step 2: Update schema baseline**

In `schema-foundation.sql`, change `participation_orders` to include:

```sql
    area_tier_id uuid not null references __schema__.area_tiers(id),
    area_sqm numeric(14,2) not null,
    booth_type varchar(60) not null,
```

Change `contracts` to include the order foreign key:

```sql
    order_id uuid not null references __schema__.participation_orders(id),
```

Add these indexes after the existing table definitions:

```sql
create unique index if not exists uidx_area_tiers_event_zone_area
    on __schema__.area_tiers(event_id, coalesce(zone_id, '00000000-0000-0000-0000-000000000000'::uuid), area_sqm);

create unique index if not exists uidx_price_rules_event_zone_booth_type_enabled
    on __schema__.price_rules(event_id, coalesce(zone_id, '00000000-0000-0000-0000-000000000000'::uuid), booth_type)
    where enabled = true;
```

Because the project does not use Flyway or Liquibase, this baseline is valid for fresh schemas. Existing local development schemas must be dropped manually before rerunning the initializer:

```powershell
$env:PGPASSWORD='postgres'
psql -h localhost -p 18182 -U postgres -d postgres -c "drop schema if exists exhibitor_sys_test cascade"
```

- [ ] **Step 3: Add demo actor constants**

Create `DemoActors.java`:

```java
package com.sportexpo.exhibitor.common;

import java.util.UUID;

public final class DemoActors {
    public static final UUID DEMO_EXHIBITOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID DEMO_ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private DemoActors() {
    }
}
```

- [ ] **Step 4: Add centralized status constants**

Create `BusinessStatuses.java`:

```java
package com.sportexpo.exhibitor.common;

public final class BusinessStatuses {
    public static final String ORDER_PENDING_SALES_CONFIRM = "PENDING_SALES_CONFIRM";
    public static final String ORDER_PENDING_CONTRACT = "PENDING_CONTRACT";
    public static final String ORDER_PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String ORDER_PARTIALLY_PAID = "PARTIALLY_PAID";
    public static final String ORDER_PAID = "PAID";
    public static final String ORDER_COMPLETED = "COMPLETED";

    public static final String PAYMENT_PENDING_FINANCE_CONFIRM = "PENDING_FINANCE_CONFIRM";
    public static final String PAYMENT_CONFIRMED = "CONFIRMED";
    public static final String PAYMENT_CONFIRM_FAILED = "CONFIRM_FAILED";

    public static final String ELIGIBILITY_NOT_OPEN = "NOT_OPEN";
    public static final String ELIGIBILITY_AVAILABLE = "AVAILABLE";
    public static final String ELIGIBILITY_PAUSED = "PAUSED";
    public static final String ELIGIBILITY_CLOSED = "CLOSED";
    public static final String PAUSE_SELECTION_PENDING = "selection_pending";

    public static final String SELECTION_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String SELECTION_CONFIRMED = "CONFIRMED";
    public static final String SELECTION_REJECTED = "REJECTED";
    public static final String SELECTION_TIMEOUT = "TIMEOUT";

    public static final String BOOTH_AVAILABLE = "AVAILABLE";
    public static final String BOOTH_LOCKED = "LOCKED";
    public static final String BOOTH_CONFIRMED = "CONFIRMED";

    public static final String CONTRACT_DRAFT = "DRAFT";
    public static final String CONTRACT_GENERATED = "GENERATED";
    public static final String CONTRACT_EFFECTIVE = "EFFECTIVE";
    public static final String CONTRACT_VERSION_UNSIGNED = "UNSIGNED";
    public static final String CONTRACT_VERSION_MARKED_SIGNED = "MARKED_SIGNED";

    public static final String LOCK_ACTIVE = "ACTIVE";
    public static final String LOCK_CONFIRMED = "CONFIRMED";
    public static final String LOCK_REJECTED = "REJECTED";
    public static final String LOCK_TIMEOUT_RELEASED = "TIMEOUT_RELEASED";

    private BusinessStatuses() {
    }
}
```

- [ ] **Step 5: Run focused test**

Run:

```powershell
cd backend
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn test -Dtest=FoundationApplicationTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add backend/src/main/resources/db/schema-foundation.sql backend/src/main/java/com/sportexpo/exhibitor/common backend/src/test/java/com/sportexpo/exhibitor/FoundationApplicationTest.java
git commit -m "chore: align second round schema baseline"
```

## Task 2: Add Pricing Area Tier and Price Rule APIs

**Files:**
- Create backend `pricing` package files listed in File Structure
- Test: `backend/src/test/java/com/sportexpo/exhibitor/pricing/PricingServiceTest.java`

- [ ] **Step 1: Write failing pricing service tests**

Create `PricingServiceTest.java`:

```java
package com.sportexpo.exhibitor.pricing;

import com.sportexpo.exhibitor.TestDatabaseSupport;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.pricing.dto.CreateAreaTierRequest;
import com.sportexpo.exhibitor.pricing.dto.CreatePriceRuleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PricingServiceTest extends TestDatabaseSupport {
    @Autowired EventService eventService;
    @Autowired AreaTierService areaTierService;
    @Autowired PriceRuleService priceRuleService;

    @Test
    void createsTierAndPriceRuleForEvent() {
        var event = eventService.create(new CreateEventRequest("体博会", 2026));

        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.00"), true));
        var price = priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true));

        assertThat(tier.areaSqm()).isEqualByComparingTo("18.00");
        assertThat(price.unitPrice()).isEqualByComparingTo("1200.00");
        assertThat(areaTierService.list(event.id())).hasSize(1);
        assertThat(priceRuleService.list(event.id())).hasSize(1);
    }

    @Test
    void rejectsDuplicateEnabledPriceRuleForSameEventZoneAndBoothType() {
        var event = eventService.create(new CreateEventRequest("体博会", 2026));
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true));

        assertThatThrownBy(() -> priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1300.00"), true)))
                .hasMessageContaining("Enabled price rule already exists");
    }
}
```

Expected: FAIL because pricing classes do not exist.

- [ ] **Step 2: Implement pricing entities and repositories**

Create `AreaTier.java` and `PriceRule.java` as JPA entities mapped to `area_tiers` and `price_rules`. Both must expose getters for every response field and update timestamps in constructors. Add repositories:

```java
public interface AreaTierRepository extends JpaRepository<AreaTier, UUID> {
    List<AreaTier> findByEventIdOrderByAreaSqmAsc(UUID eventId);
    boolean existsByEventIdAndZoneIdAndAreaSqm(UUID eventId, UUID zoneId, BigDecimal areaSqm);
}
```

```java
public interface PriceRuleRepository extends JpaRepository<PriceRule, UUID> {
    List<PriceRule> findByEventIdOrderByUpdatedAtDesc(UUID eventId);
    boolean existsByEventIdAndZoneIdAndBoothTypeAndEnabled(UUID eventId, UUID zoneId, String boothType, boolean enabled);
    Optional<PriceRule> findFirstByEventIdAndZoneIdAndBoothTypeAndEnabledOrderByUpdatedAtDesc(UUID eventId, UUID zoneId, String boothType, boolean enabled);
}
```

- [ ] **Step 3: Implement pricing DTOs and services**

Use these request records:

```java
public record CreateAreaTierRequest(
        @NotNull UUID eventId,
        UUID zoneId,
        @NotNull @DecimalMin("0.01") BigDecimal areaSqm,
        boolean enabled
) {}
```

```java
public record CreatePriceRuleRequest(
        @NotNull UUID eventId,
        UUID zoneId,
        @NotBlank String boothType,
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice,
        boolean enabled
) {}
```

`AreaTierService.create` must:

```text
1. verify event exists
2. reject duplicate eventId + zoneId + areaSqm
3. save tier
4. audit AREA_TIER_CREATE with DEMO_ADMIN_ID
```

`PriceRuleService.create` must:

```text
1. verify event exists
2. if request.enabled is true, reject duplicate enabled eventId + zoneId + boothType
3. save price rule
4. audit PRICE_RULE_CREATE with DEMO_ADMIN_ID
```

- [ ] **Step 4: Implement pricing controllers**

Expose:

```text
GET  /api/area-tiers?eventId=
POST /api/area-tiers
GET  /api/price-rules?eventId=
POST /api/price-rules
```

All endpoints return `ApiResponse`.

- [ ] **Step 5: Run pricing tests**

Run:

```powershell
cd backend
mvn test -Dtest=PricingServiceTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add backend/src/main/java/com/sportexpo/exhibitor/pricing backend/src/test/java/com/sportexpo/exhibitor/pricing/PricingServiceTest.java
git commit -m "feat: add pricing maintenance APIs"
```

## Task 3: Add Participation Orders and Selection Eligibility Creation

**Files:**
- Create backend `order` package files listed in File Structure
- Create `SelectionEligibility.java` and `SelectionEligibilityRepository.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/order/ParticipationOrderServiceTest.java`

- [ ] **Step 1: Write failing order creation tests**

Create `ParticipationOrderServiceTest.java` with:

```java
@SpringBootTest
class ParticipationOrderServiceTest extends TestDatabaseSupport {
    @Autowired EventService eventService;
    @Autowired AreaTierService areaTierService;
    @Autowired PriceRuleService priceRuleService;
    @Autowired ParticipationOrderService orderService;
    @Autowired SelectionEligibilityRepository eligibilityRepository;

    @Test
    void createsOrderWithCalculatedAmountAndNotOpenEligibility() {
        var event = eventService.create(new CreateEventRequest("体博会", 2026));
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal("18.00"), true));
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, "STANDARD", new BigDecimal("1200.00"), true));

        var order = orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, null, tier.id(), "STANDARD"
        ));

        assertThat(order.areaSqm()).isEqualByComparingTo("18.00");
        assertThat(order.areaFee()).isEqualByComparingTo("21600.00");
        assertThat(order.totalAmount()).isEqualByComparingTo("21600.00");
        assertThat(order.status()).isEqualTo(BusinessStatuses.ORDER_PENDING_SALES_CONFIRM);
        var eligibility = eligibilityRepository.findByOrderId(order.id()).orElseThrow();
        assertThat(eligibility.getStatus()).isEqualTo(BusinessStatuses.ELIGIBILITY_NOT_OPEN);
    }
}
```

Expected: FAIL because order and eligibility classes do not exist.

- [ ] **Step 2: Implement order and eligibility entities**

`ParticipationOrder` must map every column in `participation_orders`, including `areaSqm` and `boothType`.

`SelectionEligibility` must map:

```java
@Column(name = "event_id") private UUID eventId;
@Column(name = "order_id") private UUID orderId;
@Column(name = "status") private String status;
@Column(name = "pause_reason") private String pauseReason;
```

Add these mutator methods with business names:

```java
public void markAvailable(OffsetDateTime now) {
    this.status = BusinessStatuses.ELIGIBILITY_AVAILABLE;
    this.pauseReason = null;
    this.updatedAt = now;
}

public void markPaused(String pauseReason, OffsetDateTime now) {
    this.status = BusinessStatuses.ELIGIBILITY_PAUSED;
    this.pauseReason = pauseReason;
    this.updatedAt = now;
}

public void markClosed(OffsetDateTime now) {
    this.status = BusinessStatuses.ELIGIBILITY_CLOSED;
    this.pauseReason = null;
    this.updatedAt = now;
}

public void markNotOpen(OffsetDateTime now) {
    this.status = BusinessStatuses.ELIGIBILITY_NOT_OPEN;
    this.pauseReason = null;
    this.updatedAt = now;
}
```

- [ ] **Step 3: Implement order service**

`ParticipationOrderService.create` must:

```text
1. verify event exists
2. load enabled area tier
3. choose enabled price rule by zoneId first, then event-level zoneId null
4. calculate areaFee = areaSqm * unitPrice
5. set serviceFee, discountAmount, adjustmentAmount to 0.00
6. set totalAmount = areaFee
7. set status = PENDING_SALES_CONFIRM
8. save ParticipationOrder
9. save SelectionEligibility with NOT_OPEN
10. audit ORDER_CREATE with exhibitorId
```

When no price rule matches, throw `BusinessException("Enabled price rule does not exist")`.

- [ ] **Step 4: Implement order controller**

Expose:

```text
POST /api/participation-orders
GET  /api/participation-orders?eventId=&exhibitorId=&status=
```

The list endpoint supports each filter independently. When no filter is provided, return all orders for admin pages.

- [ ] **Step 5: Create reusable backend test fixture**

Create `backend/src/test/java/com/sportexpo/exhibitor/TestBusinessFixture.java`:

```java
package com.sportexpo.exhibitor;

import com.sportexpo.exhibitor.common.DemoActors;
import com.sportexpo.exhibitor.event.EventService;
import com.sportexpo.exhibitor.event.dto.CreateEventRequest;
import com.sportexpo.exhibitor.order.ParticipationOrderService;
import com.sportexpo.exhibitor.order.dto.CreateParticipationOrderRequest;
import com.sportexpo.exhibitor.order.dto.ParticipationOrderResponse;
import com.sportexpo.exhibitor.pricing.AreaTierService;
import com.sportexpo.exhibitor.pricing.PriceRuleService;
import com.sportexpo.exhibitor.pricing.dto.CreateAreaTierRequest;
import com.sportexpo.exhibitor.pricing.dto.CreatePriceRuleRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TestBusinessFixture {
    private final EventService eventService;
    private final AreaTierService areaTierService;
    private final PriceRuleService priceRuleService;
    private final ParticipationOrderService orderService;

    public TestBusinessFixture(EventService eventService, AreaTierService areaTierService,
                               PriceRuleService priceRuleService, ParticipationOrderService orderService) {
        this.eventService = eventService;
        this.areaTierService = areaTierService;
        this.priceRuleService = priceRuleService;
        this.orderService = orderService;
    }

    public ParticipationOrderResponse createPendingSalesOrder(String areaSqm, String unitPrice, String boothType) {
        var event = eventService.create(new CreateEventRequest("体博会", 2026));
        var tier = areaTierService.create(new CreateAreaTierRequest(event.id(), null, new BigDecimal(areaSqm), true));
        priceRuleService.create(new CreatePriceRuleRequest(event.id(), null, boothType, new BigDecimal(unitPrice), true));
        return orderService.create(new CreateParticipationOrderRequest(
                event.id(), DemoActors.DEMO_EXHIBITOR_ID, null, tier.id(), boothType
        ));
    }
}
```

- [ ] **Step 6: Run tests**

Run:

```powershell
cd backend
mvn test -Dtest=ParticipationOrderServiceTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/java/com/sportexpo/exhibitor/order backend/src/main/java/com/sportexpo/exhibitor/selection/SelectionEligibility.java backend/src/main/java/com/sportexpo/exhibitor/selection/SelectionEligibilityRepository.java backend/src/test/java/com/sportexpo/exhibitor/order/ParticipationOrderServiceTest.java backend/src/test/java/com/sportexpo/exhibitor/TestBusinessFixture.java
git commit -m "feat: add participation order creation"
```

## Task 4: Add Sales Confirmation and Contract Version Flow

**Files:**
- Create backend `contract` package files listed in File Structure
- Modify: `ParticipationOrderService.java`
- Modify: `ParticipationOrderController.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/contract/ContractFlowTest.java`

- [ ] **Step 1: Write failing contract flow test**

Create `ContractFlowTest.java`:

```java
@SpringBootTest
class ContractFlowTest extends TestDatabaseSupport {
    @Autowired TestBusinessFixture fixture;
    @Autowired ParticipationOrderService orderService;
    @Autowired ContractService contractService;
    @Autowired ContractRepository contractRepository;
    @Autowired ContractVersionRepository contractVersionRepository;

    @Test
    void salesConfirmGeneratesContractAndMarkEffectiveMovesOrderToPayment() {
        var order = fixture.createPendingSalesOrder("18.00", "1200.00", "STANDARD");

        var confirmed = orderService.confirmSales(order.id(), DemoActors.DEMO_ADMIN_ID);

        assertThat(confirmed.status()).isEqualTo(BusinessStatuses.ORDER_PENDING_CONTRACT);
        var contract = contractRepository.findByOrderId(order.id()).orElseThrow();
        assertThat(contract.getStatus()).isEqualTo(BusinessStatuses.CONTRACT_GENERATED);
        var versions = contractVersionRepository.findByContractIdOrderByVersionNoAsc(contract.getId());
        assertThat(versions).hasSize(1);
        assertThat(versions.get(0).getSignStatus()).isEqualTo(BusinessStatuses.CONTRACT_VERSION_UNSIGNED);

        var effective = contractService.markOfflineSigned(contract.getId(), DemoActors.DEMO_ADMIN_ID);

        assertThat(effective.status()).isEqualTo(BusinessStatuses.CONTRACT_EFFECTIVE);
        assertThat(orderService.get(order.id()).status()).isEqualTo(BusinessStatuses.ORDER_PENDING_PAYMENT);
        assertThat(contractVersionRepository.findByContractIdOrderByVersionNoAsc(contract.getId()).get(0).getSignStatus())
                .isEqualTo(BusinessStatuses.CONTRACT_VERSION_MARKED_SIGNED);
    }
}
```

- [ ] **Step 2: Implement contract entities**

`contracts` currently lacks `order_id`. Add `order_id uuid not null references __schema__.participation_orders(id)` to schema and map it in `Contract`.

`ContractVersion.contentSnapshot` may be mapped as `String` with column definition:

```java
@Column(name = "content_snapshot", columnDefinition = "jsonb")
private String contentSnapshot;
```

The generated v1 snapshot string must be valid JSON:

```json
{"version":1,"reason":"sales_confirm","source":"offline_contract","signMode":"offline_mark"}
```

Add repository methods:

```java
public interface ContractRepository extends JpaRepository<Contract, UUID> {
    Optional<Contract> findByOrderId(UUID orderId);
    List<Contract> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
    List<Contract> findAllByOrderByCreatedAtDesc();
}
```

```java
public interface ContractVersionRepository extends JpaRepository<ContractVersion, UUID> {
    List<ContractVersion> findByContractIdOrderByVersionNoAsc(UUID contractId);
}
```

- [ ] **Step 3: Implement sales confirmation**

`ParticipationOrderService.confirmSales(orderId, actorId)` must run in one transaction:

```text
1. load order
2. require status PENDING_SALES_CONFIRM
3. set order status to PENDING_CONTRACT
4. save order
5. create Contract with status GENERATED, orderId, eventId, contractNo = "CON-" + yyyyMMddHHmmss + "-" + first 8 chars order UUID
6. create ContractVersion v1 with signStatus UNSIGNED, amount, areaSqm, createdBy actorId
7. set contract.currentVersionId to v1 id
8. audit ORDER_SALES_CONFIRM and CONTRACT_GENERATE
```

- [ ] **Step 4: Implement offline contract marking**

`ContractService.markOfflineSigned(contractId, actorId)` must run in one transaction:

```text
1. load contract
2. require status GENERATED
3. lock and load related order
4. require order status PENDING_CONTRACT
5. set contract status EFFECTIVE
6. set current version signStatus MARKED_SIGNED, signerId actorId, signedAt now
7. set order status PENDING_PAYMENT
8. audit CONTRACT_MARK_SIGNED and ORDER_CONTRACT_EFFECTIVE
```

- [ ] **Step 5: Implement contract controllers**

Expose:

```text
GET  /api/contracts?orderId=
GET  /api/contracts/{contractId}/versions
POST /api/contracts/{contractId}/mark-offline-signed
POST /api/participation-orders/{orderId}/confirm
```

When `orderId` is omitted, `GET /api/contracts` returns all contracts ordered by `createdAt desc` for the admin contract page.

- [ ] **Step 6: Run tests**

Run:

```powershell
cd backend
mvn test -Dtest=ContractFlowTest,ParticipationOrderServiceTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/resources/db/schema-foundation.sql backend/src/main/java/com/sportexpo/exhibitor/contract backend/src/main/java/com/sportexpo/exhibitor/order backend/src/test/java/com/sportexpo/exhibitor/contract backend/src/test/java/com/sportexpo/exhibitor/TestBusinessFixture.java
git commit -m "feat: add sales confirmation contract flow"
```

## Task 5: Add Payments and Finance Confirmation Aggregation

**Files:**
- Create backend `payment` package files listed in File Structure
- Modify: `ParticipationOrderRepository.java`
- Modify: `SelectionEligibilityRepository.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/payment/PaymentServiceTest.java`

- [ ] **Step 1: Write failing payment tests**

Create tests for partial and full payment:

```java
@SpringBootTest
class PaymentServiceTest extends TestDatabaseSupport {
    @Autowired TestBusinessFixture fixture;
    @Autowired PaymentService paymentService;
    @Autowired ParticipationOrderService orderService;
    @Autowired SelectionEligibilityRepository eligibilityRepository;

    @Test
    void partialPaymentKeepsEligibilityClosedAndFullPaymentOpensSelection() {
        var order = fixture.createPendingPaymentOrder("18.00", "1200.00", "STANDARD");

        var p1 = paymentService.create(new CreatePaymentRequest(order.id(), new BigDecimal("10000.00"), "OFFLINE_TRANSFER"));
        paymentService.confirm(p1.id(), DemoActors.DEMO_ADMIN_ID);

        assertThat(orderService.get(order.id()).status()).isEqualTo(BusinessStatuses.ORDER_PARTIALLY_PAID);
        assertThat(eligibilityRepository.findByOrderId(order.id()).orElseThrow().getStatus())
                .isEqualTo(BusinessStatuses.ELIGIBILITY_NOT_OPEN);

        var p2 = paymentService.create(new CreatePaymentRequest(order.id(), new BigDecimal("11600.00"), "OFFLINE_TRANSFER"));
        paymentService.confirm(p2.id(), DemoActors.DEMO_ADMIN_ID);

        assertThat(orderService.get(order.id()).status()).isEqualTo(BusinessStatuses.ORDER_PAID);
        assertThat(eligibilityRepository.findByOrderId(order.id()).orElseThrow().getStatus())
                .isEqualTo(BusinessStatuses.ELIGIBILITY_AVAILABLE);
    }
}
```

Before running the tests, extend `TestBusinessFixture` with:

```java
public ParticipationOrderResponse createPendingPaymentOrder(String areaSqm, String unitPrice, String boothType) {
    var order = createPendingSalesOrder(areaSqm, unitPrice, boothType);
    var confirmed = orderService.confirmSales(order.id(), DemoActors.DEMO_ADMIN_ID);
    var contract = contractRepository.findByOrderId(confirmed.id()).orElseThrow();
    contractService.markOfflineSigned(contract.getId(), DemoActors.DEMO_ADMIN_ID);
    return orderService.get(order.id());
}
```

Add `ContractRepository` and `ContractService` constructor dependencies to `TestBusinessFixture`.

Add a concurrency test:

```java
@Test
void concurrentFinanceConfirmationsConvergeToPaid() throws Exception {
    var order = fixture.createPendingPaymentOrder("18.00", "1200.00", "STANDARD");
    var p1 = paymentService.create(new CreatePaymentRequest(order.id(), new BigDecimal("10000.00"), "OFFLINE_TRANSFER"));
    var p2 = paymentService.create(new CreatePaymentRequest(order.id(), new BigDecimal("11600.00"), "OFFLINE_TRANSFER"));

    var pool = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    var done = new CountDownLatch(2);
    var errors = new CopyOnWriteArrayList<Throwable>();

    for (var paymentId : List.of(p1.id(), p2.id())) {
        pool.submit(() -> {
            try {
                start.await();
                paymentService.confirm(paymentId, DemoActors.DEMO_ADMIN_ID);
            } catch (Throwable error) {
                errors.add(error);
            } finally {
                done.countDown();
            }
        });
    }

    start.countDown();
    assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    pool.shutdownNow();

    assertThat(errors).isEmpty();
    assertThat(orderService.get(order.id()).status()).isEqualTo(BusinessStatuses.ORDER_PAID);
}
```

- [ ] **Step 2: Add locking repository methods**

In `ParticipationOrderRepository`:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select o from ParticipationOrder o where o.id = :id")
Optional<ParticipationOrder> findByIdForUpdate(@Param("id") UUID id);
```

In `PaymentRepository`:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select p from Payment p where p.id = :id")
Optional<Payment> findByIdForUpdate(@Param("id") UUID id);

List<Payment> findByOrderIdAndStatus(UUID orderId, String status);
List<Payment> findByOrderId(UUID orderId);
```

- [ ] **Step 3: Implement payment create**

`PaymentService.create` must:

```text
1. load order
2. require order status PENDING_PAYMENT or PARTIALLY_PAID
3. require amount > 0
4. save payment with status PENDING_FINANCE_CONFIRM
5. audit PAYMENT_CREATE with DEMO_EXHIBITOR_ID
```

- [ ] **Step 4: Implement finance confirmation with exact transaction order**

`PaymentService.confirm(paymentId, actorId)` must follow this exact sequence:

```text
1. begin tx
2. SELECT * FROM participation_orders WHERE id = ? FOR UPDATE
3. SELECT * FROM payments WHERE id = ? FOR UPDATE
4. UPDATE payments SET status = 'CONFIRMED', confirmed_at = now, updated_by = actorId WHERE id = ?
5. SELECT sum(amount) FROM payments WHERE order_id = ? AND status = 'CONFIRMED'
6. compute new order.status and selection_eligibility.status
7. UPDATE participation_orders
8. UPDATE selection_eligibilities
9. INSERT audit_logs
10. commit
```

In JPA service code this means:

```java
@Transactional
public PaymentResponse confirm(UUID paymentId, UUID actorId) {
    var orderId = paymentRepository.findOrderIdByPaymentId(paymentId)
            .orElseThrow(() -> new BusinessException("Payment does not exist"));
    var order = orderRepository.findByIdForUpdate(orderId)
            .orElseThrow(() -> new BusinessException("Order does not exist"));
    var payment = paymentRepository.findByIdForUpdate(paymentId)
            .orElseThrow(() -> new BusinessException("Payment does not exist"));
    if (!payment.getOrderId().equals(order.getId())) {
        throw new BusinessException("Payment does not belong to locked order");
    }
    if (!BusinessStatuses.PAYMENT_PENDING_FINANCE_CONFIRM.equals(payment.getStatus())) {
        throw new BusinessException("Payment is not pending finance confirmation");
    }
    payment.markConfirmed(actorId, OffsetDateTime.now());
    paymentRepository.save(payment);

    var confirmedAmount = paymentRepository.findByOrderIdAndStatus(order.getId(), BusinessStatuses.PAYMENT_CONFIRMED)
            .stream()
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    var eligibility = eligibilityRepository.findByOrderId(order.getId())
            .orElseThrow(() -> new BusinessException("Selection eligibility does not exist"));

    if (confirmedAmount.compareTo(order.getTotalAmount()) >= 0) {
        order.markPaid(OffsetDateTime.now(), actorId);
        eligibility.markAvailable(OffsetDateTime.now());
    } else if (confirmedAmount.compareTo(BigDecimal.ZERO) > 0) {
        order.markPartiallyPaid(OffsetDateTime.now(), actorId);
        eligibility.markNotOpen(OffsetDateTime.now());
    }

    auditService.record(order.getEventId(), actorId, "PAYMENT_CONFIRM", "Payment", payment.getId(),
            BusinessStatuses.PAYMENT_PENDING_FINANCE_CONFIRM, BusinessStatuses.PAYMENT_CONFIRMED, "finance confirm payment");
    return toResponse(payment);
}
```

Add this repository method for the initial order-id lookup. It must not use `@Lock`; it exists only so the service can acquire the order lock before acquiring the payment lock:

```java
@Query("select p.orderId from Payment p where p.id = :paymentId")
Optional<UUID> findOrderIdByPaymentId(@Param("paymentId") UUID paymentId);
```

- [ ] **Step 5: Implement payment fail**

`PaymentService.fail(paymentId, actorId)` locks the payment row, requires `PENDING_FINANCE_CONFIRM`, marks status `CONFIRM_FAILED`, and writes audit `PAYMENT_FAIL`. It does not change order or eligibility status.

- [ ] **Step 6: Implement payment controller**

Expose:

```text
POST /api/payments
GET  /api/payments?orderId=&status=
POST /api/payments/{paymentId}/confirm
POST /api/payments/{paymentId}/fail
```

- [ ] **Step 7: Run tests**

Run:

```powershell
cd backend
mvn test -Dtest=PaymentServiceTest
```

Expected: PASS, including the concurrency test.

- [ ] **Step 8: Commit**

```powershell
git add backend/src/main/java/com/sportexpo/exhibitor/payment backend/src/main/java/com/sportexpo/exhibitor/order backend/src/main/java/com/sportexpo/exhibitor/selection backend/src/test/java/com/sportexpo/exhibitor/payment/PaymentServiceTest.java
git commit -m "feat: add payment confirmation aggregation"
```

## Task 6: Extend Booth Listing and Add Locking Support

**Files:**
- Modify: `Booth.java`
- Modify: `BoothRepository.java`
- Modify: `BoothService.java`
- Modify: `BoothController.java`
- Modify: `BoothResponse.java`
- Test: `BoothServiceTest.java`

- [ ] **Step 1: Write failing status filter test**

In `BoothServiceTest`, add:

```java
@Test
void listsBoothsByEventAndStatus() {
    var event = eventService.create(new CreateEventRequest("体博会", 2026));
    boothService.create(new CreateBoothRequest(event.id(), "A001", new BigDecimal("9.00"), "STANDARD"));
    boothService.create(new CreateBoothRequest(event.id(), "A002", new BigDecimal("9.00"), "CUSTOM"));

    var available = boothService.list(event.id(), BusinessStatuses.BOOTH_AVAILABLE);

    assertThat(available).hasSize(2);
    assertThat(available).allMatch(booth -> booth.status().equals(BusinessStatuses.BOOTH_AVAILABLE));
}
```

- [ ] **Step 2: Complete booth mappings**

Add mapped fields for `pavilionId`, `zoneId`, `x`, `y`, `width`, `height`, and `updatedBy` to `Booth`. Add these setters with business names:

```java
public void markLocked(OffsetDateTime now, UUID actorId) {
    this.status = BusinessStatuses.BOOTH_LOCKED;
    this.updatedAt = now;
    this.updatedBy = actorId;
}

public void markAvailable(OffsetDateTime now, UUID actorId) {
    this.status = BusinessStatuses.BOOTH_AVAILABLE;
    this.updatedAt = now;
    this.updatedBy = actorId;
}

public void markConfirmed(OffsetDateTime now, UUID actorId) {
    this.status = BusinessStatuses.BOOTH_CONFIRMED;
    this.updatedAt = now;
    this.updatedBy = actorId;
}
```

- [ ] **Step 3: Add repository locking methods**

Add:

```java
List<Booth> findByEventIdAndStatus(UUID eventId, String status);

@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select b from Booth b where b.id in :ids order by b.id asc")
List<Booth> findAllByIdInForUpdateOrderById(@Param("ids") List<UUID> ids);
```

The query order is part of the deadlock prevention strategy. Do not replace it with request-order iteration.

- [ ] **Step 4: Update service and controller**

Change list signatures to:

```java
public List<BoothResponse> list(UUID eventId, String status)
```

Controller:

```java
@GetMapping
public ApiResponse<List<BoothResponse>> list(@RequestParam UUID eventId,
                                             @RequestParam(required = false) String status) {
    return ApiResponse.ok(service.list(eventId, status));
}
```

- [ ] **Step 5: Run tests**

Run:

```powershell
cd backend
mvn test -Dtest=BoothServiceTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add backend/src/main/java/com/sportexpo/exhibitor/booth backend/src/test/java/com/sportexpo/exhibitor/booth/BoothServiceTest.java
git commit -m "feat: add booth status filter and locks"
```

## Task 7: Add Booth Selection Submission

**Files:**
- Create selection package files listed in File Structure
- Modify: `BoothRepository.java`
- Test: `backend/src/test/java/com/sportexpo/exhibitor/selection/BoothSelectionServiceTest.java`

- [ ] **Step 1: Write failing area validation tests**

Add tests:

```java
@Test
void rejectsSelectionWhenAreaIsNotExactlyEqual() {
    var order = fixture.createPaidOrder("18.00", "1200.00", "STANDARD");
    var booth = fixture.createBooth(order.eventId(), "A001", "9.00", "STANDARD");

    assertThatThrownBy(() -> selectionService.submit(new CreateBoothSelectionRequest(
            order.id(), order.exhibitorId(), List.of(booth.id())
    )))
            .hasMessageContaining("Selected booth area must equal order area");
}

@Test
void submitsSelectionAndLocksBoothsWhenAreaMatches() {
    var order = fixture.createPaidOrder("18.00", "1200.00", "STANDARD");
    var b1 = fixture.createBooth(order.eventId(), "A001", "9.00", "STANDARD");
    var b2 = fixture.createBooth(order.eventId(), "A002", "9.00", "STANDARD");

    var selection = selectionService.submit(new CreateBoothSelectionRequest(
            order.id(), order.exhibitorId(), List.of(b2.id(), b1.id())
    ));

    assertThat(selection.status()).isEqualTo(BusinessStatuses.SELECTION_PENDING_REVIEW);
    assertThat(selection.totalAreaSqm()).isEqualByComparingTo("18.00");
    assertThat(boothService.get(b1.id()).status()).isEqualTo(BusinessStatuses.BOOTH_LOCKED);
    assertThat(eligibilityRepository.findByOrderId(order.id()).orElseThrow().getStatus())
            .isEqualTo(BusinessStatuses.ELIGIBILITY_PAUSED);
}
```

Before running these tests, extend `TestBusinessFixture` with:

```java
public ParticipationOrderResponse createPaidOrder(String areaSqm, String unitPrice, String boothType) {
    var order = createPendingPaymentOrder(areaSqm, unitPrice, boothType);
    paymentService.confirm(
            paymentService.create(new CreatePaymentRequest(order.id(), order.totalAmount(), "OFFLINE_TRANSFER")).id(),
            DemoActors.DEMO_ADMIN_ID
    );
    return orderService.get(order.id());
}

public BoothResponse createBooth(UUID eventId, String boothNo, String areaSqm, String boothType) {
    return boothService.create(new CreateBoothRequest(eventId, boothNo, new BigDecimal(areaSqm), boothType));
}
```

Add `PaymentService`, `BoothService`, and their DTO imports as constructor dependencies to `TestBusinessFixture`.

Add booth type validation:

```java
@Test
void rejectsSelectionWhenBoothTypeDoesNotMatchOrder() {
    var order = fixture.createPaidOrder("9.00", "1200.00", "STANDARD");
    var booth = fixture.createBooth(order.eventId(), "C001", "9.00", "CUSTOM");

    assertThatThrownBy(() -> selectionService.submit(new CreateBoothSelectionRequest(
            order.id(), order.exhibitorId(), List.of(booth.id())
    ))).hasMessageContaining("Selected booth type must match order booth type");
}
```

- [ ] **Step 2: Write failing concurrent conflict test**

```java
@Test
void onlyOneConcurrentSelectionCanLockSameBooth() throws Exception {
    var order1 = fixture.createPaidOrder("9.00", "1200.00", "STANDARD");
    var order2 = fixture.createPaidOrder("9.00", "1200.00", "STANDARD");
    var booth = fixture.createBooth(order1.eventId(), "A009", "9.00", "STANDARD");

    var pool = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    var successes = new AtomicInteger();
    var failures = new AtomicInteger();

    for (var order : List.of(order1, order2)) {
        pool.submit(() -> {
            try {
                start.await();
                selectionService.submit(new CreateBoothSelectionRequest(order.id(), order.exhibitorId(), List.of(booth.id())));
                successes.incrementAndGet();
            } catch (Exception expected) {
                failures.incrementAndGet();
            }
        });
    }

    start.countDown();
    pool.shutdown();
    assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    assertThat(successes.get()).isEqualTo(1);
    assertThat(failures.get()).isEqualTo(1);
}
```

- [ ] **Step 3: Implement selection entities and repositories**

`BoothSelection` maps `booth_selections`. `BoothLock` maps `booth_locks`.

Repository methods:

```java
boolean existsByOrderIdAndStatus(UUID orderId, String status);
List<BoothSelection> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
List<BoothSelection> findByStatusAndLockExpiresAtBefore(String status, OffsetDateTime now);
```

`BoothLockRepository`:

```java
List<BoothLock> findBySelectionId(UUID selectionId);
```

- [ ] **Step 4: Implement selection submit transaction**

`BoothSelectionService.submit` must run in one transaction:

```text
1. load order
2. require order.exhibitorId equals request.exhibitorId
3. require order.status PAID
4. load eligibility and require AVAILABLE
5. reject if order already has PENDING_REVIEW selection
6. sort boothIds by UUID ascending
7. SELECT booth rows FOR UPDATE in sorted order
8. require all requested booths were found
9. require every booth.eventId equals order.eventId
10. require every booth.status AVAILABLE
11. require every booth.boothType equals order.boothType
12. sum booth.areaSqm with BigDecimal
13. require sum equals order.areaSqm
14. create BoothSelection status PENDING_REVIEW with lockStartedAt now and lockExpiresAt now + event.defaultLockMinutes
15. create BoothLock rows status ACTIVE
16. mark booths LOCKED
17. mark eligibility PAUSED with pauseReason selection_pending
18. audit BOOTH_SELECTION_SUBMIT
```

- [ ] **Step 5: Implement selection controller**

Expose:

```text
GET  /api/selection-eligibilities?orderId=
POST /api/booth-selections
GET  /api/booth-selections?orderId=&status=
```

- [ ] **Step 6: Run tests**

Run:

```powershell
cd backend
mvn test -Dtest=BoothSelectionServiceTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/java/com/sportexpo/exhibitor/selection backend/src/test/java/com/sportexpo/exhibitor/selection/BoothSelectionServiceTest.java
git commit -m "feat: add booth selection submission"
```

## Task 8: Add Selection Approve, Reject, and Timeout Release

**Files:**
- Modify: `BoothSelectionService.java`
- Modify: `BoothSelectionController.java`
- Test: `BoothSelectionServiceTest.java`

- [ ] **Step 1: Write failing review tests**

Add:

```java
@Test
void approveSelectionConfirmsBoothsAndClosesEligibility() {
    var selection = fixture.createPendingSelection("18.00", "STANDARD");

    var approved = selectionService.approve(selection.id(), DemoActors.DEMO_ADMIN_ID);

    assertThat(approved.status()).isEqualTo(BusinessStatuses.SELECTION_CONFIRMED);
    assertThat(selectionService.listByOrder(selection.orderId(), null)).first()
            .extracting(BoothSelectionResponse::status)
            .isEqualTo(BusinessStatuses.SELECTION_CONFIRMED);
}

@Test
void rejectSelectionReleasesBoothsAndReopensEligibility() {
    var selection = fixture.createPendingSelection("18.00", "STANDARD");

    var rejected = selectionService.reject(selection.id(), new RejectBoothSelectionRequest("展位位置不符合规则"), DemoActors.DEMO_ADMIN_ID);

    assertThat(rejected.status()).isEqualTo(BusinessStatuses.SELECTION_REJECTED);
    assertThat(eligibilityRepository.findByOrderId(selection.orderId()).orElseThrow().getStatus())
            .isEqualTo(BusinessStatuses.ELIGIBILITY_AVAILABLE);
}
```

Before running these tests, extend `TestBusinessFixture` with:

```java
public BoothSelectionResponse createPendingSelection(String areaSqm, String boothType) {
    var order = createPaidOrder(areaSqm, "1200.00", boothType);
    var boothArea = new BigDecimal(areaSqm).divide(new BigDecimal("2.00"));
    var b1 = createBooth(order.eventId(), "S" + UUID.randomUUID().toString().substring(0, 6), boothArea.toPlainString(), boothType);
    var b2 = createBooth(order.eventId(), "S" + UUID.randomUUID().toString().substring(0, 6), boothArea.toPlainString(), boothType);
    return boothSelectionService.submit(new CreateBoothSelectionRequest(order.id(), order.exhibitorId(), List.of(b1.id(), b2.id())));
}

public BoothSelectionResponse createExpiredPendingSelection(String areaSqm, String boothType) {
    var selection = createPendingSelection(areaSqm, boothType);
    boothSelectionRepository.findById(selection.id()).orElseThrow()
            .forceLockExpiresAtForTest(OffsetDateTime.now().minusMinutes(1));
    return selection;
}
```

Add `BoothSelectionService` and `BoothSelectionRepository` constructor dependencies to `TestBusinessFixture`. Add a package-visible test helper method `forceLockExpiresAtForTest(OffsetDateTime value)` on `BoothSelection`; this method is only for tests and should not be used by production services.

Add timeout test:

```java
@Test
void timeoutReleaseReleasesBoothsAndReopensEligibility() {
    var selection = fixture.createExpiredPendingSelection("18.00", "STANDARD");

    var released = selectionService.timeoutRelease(selection.id(), DemoActors.DEMO_ADMIN_ID);

    assertThat(released.status()).isEqualTo(BusinessStatuses.SELECTION_TIMEOUT);
    assertThat(eligibilityRepository.findByOrderId(selection.orderId()).orElseThrow().getStatus())
            .isEqualTo(BusinessStatuses.ELIGIBILITY_AVAILABLE);
}
```

- [ ] **Step 2: Implement approve transaction**

`approve(selectionId, actorId)`:

```text
1. lock selection row
2. require status PENDING_REVIEW
3. load locks
4. lock booths by UUID ascending
5. require booths are LOCKED
6. mark selection CONFIRMED with reviewerUserId, reviewedAt
7. mark locks CONFIRMED
8. mark booths CONFIRMED
9. mark eligibility CLOSED
10. audit BOOTH_SELECTION_APPROVE
```

- [ ] **Step 3: Implement reject transaction**

`reject(selectionId, request, actorId)`:

```text
1. lock selection row
2. require status PENDING_REVIEW
3. require request.reason is not blank
4. load locks
5. lock booths by UUID ascending
6. mark selection REJECTED with reviewerUserId, reviewedAt, reviewComment
7. mark locks REJECTED
8. mark booths AVAILABLE
9. mark eligibility AVAILABLE
10. audit BOOTH_SELECTION_REJECT
```

- [ ] **Step 4: Implement timeout release transaction**

`timeoutRelease(selectionId, actorId)`:

```text
1. lock selection row
2. require status PENDING_REVIEW
3. require now is after lockExpiresAt
4. load locks
5. lock booths by UUID ascending
6. mark selection TIMEOUT with reviewComment "lock timeout released"
7. mark locks TIMEOUT_RELEASED
8. mark booths AVAILABLE
9. mark eligibility AVAILABLE
10. audit BOOTH_SELECTION_TIMEOUT_RELEASE
```

- [ ] **Step 5: Implement controller actions**

Expose:

```text
POST /api/booth-selections/{selectionId}/approve
POST /api/booth-selections/{selectionId}/reject
POST /api/booth-selections/{selectionId}/timeout-release
```

- [ ] **Step 6: Run tests**

Run:

```powershell
cd backend
mvn test -Dtest=BoothSelectionServiceTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/java/com/sportexpo/exhibitor/selection backend/src/test/java/com/sportexpo/exhibitor/selection/BoothSelectionServiceTest.java
git commit -m "feat: add selection review actions"
```

## Task 9: Add Frontend Request Helpers and Demo Actor Constants

**Files:**
- Modify: `frontend/src/app/request.ts`
- Create: `frontend/src/app/demoActors.ts`

- [ ] **Step 1: Add POST helper**

Modify `request.ts` to export:

```ts
export async function apiPost<T, B = unknown>(url: string, body?: B): Promise<T> {
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  const parsed = await parseApiResponse<T>(response);
  return parsed;
}
```

Refactor `apiGet` to use the same private `parseApiResponse<T>(response)` function that already handles non-2xx `ApiResponse.message`.

- [ ] **Step 2: Add demo actors**

Create `demoActors.ts`:

```ts
export const DEMO_EXHIBITOR_ID = '00000000-0000-0000-0000-000000000001';
export const DEMO_ADMIN_ID = '00000000-0000-0000-0000-000000000002';
```

- [ ] **Step 3: Run frontend build**

Run:

```powershell
cd frontend
cmd /c npm run build
```

Expected: PASS.

- [ ] **Step 4: Commit**

```powershell
git add frontend/src/app/request.ts frontend/src/app/demoActors.ts
git commit -m "feat: add frontend write request helper"
```

## Task 10: Add Admin ProComponents Pages

**Files:**
- Create admin frontend pages listed in File Structure
- Modify: `routes.tsx`
- Modify: `AdminLayout.tsx`

- [ ] **Step 1: Add routes and menu**

Add lazy imports and routes:

```tsx
const AreaTierListPage = lazy(() => import('../pages/admin/pricing/AreaTierListPage'));
const PriceRuleListPage = lazy(() => import('../pages/admin/pricing/PriceRuleListPage'));
const AdminOrderListPage = lazy(() => import('../pages/admin/orders/AdminOrderListPage'));
const ContractListPage = lazy(() => import('../pages/admin/contracts/ContractListPage'));
const PaymentConfirmPage = lazy(() => import('../pages/admin/payments/PaymentConfirmPage'));
const SelectionReviewPage = lazy(() => import('../pages/admin/selections/SelectionReviewPage'));
```

Routes:

```tsx
{ path: 'area-tiers', element: lazyPage(<AreaTierListPage />) },
{ path: 'price-rules', element: lazyPage(<PriceRuleListPage />) },
{ path: 'orders', element: lazyPage(<AdminOrderListPage />) },
{ path: 'contracts', element: lazyPage(<ContractListPage />) },
{ path: 'payments', element: lazyPage(<PaymentConfirmPage />) },
{ path: 'selections', element: lazyPage(<SelectionReviewPage />) },
```

Menu labels:

```text
面积档位
价格规则
订单管理
合同管理
付款确认
选位审核
```

- [ ] **Step 2: Add area tier page**

Use `PageContainer`, `ProTable`, and `ModalForm`. The table request calls `/api/area-tiers?eventId=${encodeURIComponent(eventId)}`. The modal has `ProFormText` for eventId, optional zoneId, `ProFormDigit` for areaSqm, and `ProFormSwitch` for enabled. Submit with `apiPost('/api/area-tiers', values)`.

- [ ] **Step 3: Add price rule page**

Use `PageContainer`, `ProTable`, and `ModalForm`. The modal has eventId, optional zoneId, boothType select (`STANDARD`, `CUSTOM`), unitPrice digit, enabled switch. Submit with `apiPost('/api/price-rules', values)`.

- [ ] **Step 4: Add admin order page**

Use `ProTable` against `/api/participation-orders`. Add action buttons:

```text
销售确认 -> POST /api/participation-orders/{id}/confirm
```

After action succeeds, call `actionRef.current?.reload()`.

- [ ] **Step 5: Add contract page**

Use `ProTable` against `/api/contracts`. Add action button:

```text
标记线下签署 -> POST /api/contracts/{id}/mark-offline-signed
```

Add `DrawerForm` or `Drawer` containing versions loaded from `/api/contracts/{contractId}/versions`.

- [ ] **Step 6: Add payment confirmation page**

Use `ProTable` against `/api/payments?status=PENDING_FINANCE_CONFIRM`. Add actions:

```text
确认到账 -> POST /api/payments/{id}/confirm
确认失败 -> POST /api/payments/{id}/fail
```

- [ ] **Step 7: Add selection review page**

Use `ProTable` against `/api/booth-selections?status=PENDING_REVIEW`. Add actions:

```text
审核通过 -> POST /api/booth-selections/{id}/approve
驳回 -> ModalForm reason then POST /api/booth-selections/{id}/reject
超时释放 -> POST /api/booth-selections/{id}/timeout-release
```

- [ ] **Step 8: Run frontend build**

Run:

```powershell
cd frontend
cmd /c npm run build
```

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add frontend/src/app/routes.tsx frontend/src/app/layouts/AdminLayout.tsx frontend/src/pages/admin
git commit -m "feat: add admin order payment selection pages"
```

## Task 11: Add Exhibitor Order, Payment, and Selection Pages

**Files:**
- Create exhibitor frontend pages listed in File Structure
- Modify: `routes.tsx`
- Modify: `ExhibitorLayout.tsx`

- [ ] **Step 1: Add exhibitor routes and menu**

Routes:

```tsx
{ path: 'orders/new', element: lazyPage(<OrderCreatePage />) },
{ path: 'orders', element: lazyPage(<MyOrdersPage />) },
{ path: 'payments/new', element: lazyPage(<PaymentSubmitPage />) },
{ path: 'selections/new', element: lazyPage(<BoothSelectionPage />) },
{ path: 'selections', element: lazyPage(<SelectionResultPage />) },
```

Menu labels:

```text
展商首页
我要下单
我的订单
提交付款
选择展位
选位结果
```

- [ ] **Step 2: Add order creation page**

Use `PageContainer` and `ProForm`. Fields:

```text
eventId
areaTierId
boothType select STANDARD/CUSTOM
zoneId optional
```

Submit body:

```ts
{
  eventId: values.eventId,
  exhibitorId: DEMO_EXHIBITOR_ID,
  zoneId: values.zoneId || null,
  areaTierId: values.areaTierId,
  boothType: values.boothType
}
```

Call `apiPost('/api/participation-orders', body)` and show created order number, status, and total amount with `message.success`.

- [ ] **Step 3: Add my orders page**

Use `ProTable` against:

```ts
`/api/participation-orders?exhibitorId=${DEMO_EXHIBITOR_ID}`
```

Show columns:

```text
eventId, areaSqm, boothType, totalAmount, status
```

Use row action links to `/exhibitor/payments/new?orderId=${id}` and `/exhibitor/selections/new?orderId=${id}`. Disable selection link unless `status === 'PAID'`.

- [ ] **Step 4: Add payment submit page**

Use `ProForm` with:

```text
orderId
amount
method default OFFLINE_TRANSFER
```

Submit to `/api/payments`.

- [ ] **Step 5: Add booth selection page**

Use `PageContainer`, `ProTable`, `Statistic`, and `Button`.

Flow:

```text
1. user enters orderId
2. load order detail through /api/participation-orders?exhibitorId=DEMO_EXHIBITOR_ID and find id
3. load booths with /api/booths?eventId={order.eventId}&status=AVAILABLE
4. enable rowSelection
5. compute selectedArea with BigDecimal-safe decimal strings converted to Number only for display
6. disable submit unless selectedArea.toFixed(2) equals order.areaSqm.toFixed(2)
7. POST /api/booth-selections with orderId, exhibitorId, boothIds
```

Use a visible Ant Design `Alert` when area is short or over. The backend remains the source of truth for strict area equality.

- [ ] **Step 6: Add selection result page**

Use `ProTable` against `/api/booth-selections?orderId=${encodeURIComponent(orderId)}`. Show all statuses including rejected and timeout. Do not hide old selections.

- [ ] **Step 7: Run frontend build**

Run:

```powershell
cd frontend
cmd /c npm run build
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add frontend/src/app/routes.tsx frontend/src/app/layouts/ExhibitorLayout.tsx frontend/src/pages/exhibitor frontend/src/app/demoActors.ts
git commit -m "feat: add exhibitor order payment selection pages"
```

## Task 12: Full Backend API and Frontend Smoke Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Run all backend tests**

Run:

```powershell
cd backend
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys_test'
mvn test
```

Expected: all tests PASS.

- [ ] **Step 2: Run frontend build**

Run:

```powershell
cd frontend
cmd /c npm run build
```

Expected: PASS.

- [ ] **Step 3: Run manual API smoke chain**

Start backend with:

```powershell
cd backend
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:18182/postgres'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='postgres'
$env:APP_DB_SCHEMA='exhibitor_sys'
mvn spring-boot:run
```

Start frontend with:

```powershell
cd frontend
cmd /c npm run dev
```

Open the frontend and verify this chain:

```text
1. admin creates or reuses an event and booths
2. admin creates area tier 18.00
3. admin creates STANDARD price rule
4. exhibitor creates order
5. admin sales-confirms order
6. admin marks contract offline signed
7. exhibitor submits payment
8. admin confirms payment
9. exhibitor selects two 9.00 STANDARD booths
10. admin approves selection
```

- [ ] **Step 4: Update README**

Add a "Second-round business smoke flow" section listing the same 10 steps and the demo actor IDs.

- [ ] **Step 5: Commit**

```powershell
git add README.md
git commit -m "docs: add second round smoke flow"
```

## Self-Review Checklist

- [ ] The plan implements every in-scope item from `2026-06-13-order-contract-payment-selection-design.md`.
- [ ] Payment confirmation task explicitly locks order first and payment second.
- [ ] Booth selection task locks booth rows in stable UUID ascending order.
- [ ] Area equality is strict and enforced in backend service tests.
- [ ] Booth type consistency is tested and enforced.
- [ ] Contract status and contract version sign status are implemented.
- [ ] Selection approval, rejection, and timeout release are all implemented.
- [ ] Selection history is retained and visible through list APIs.
- [ ] `GET /api/booths?eventId=&status=AVAILABLE` is supported.
- [ ] Demo exhibitor and admin UUIDs are centralized.
- [ ] Frontend pages use Ant Design and ProComponents; no custom base table, form, or modal components are introduced.
- [ ] Backend tests include the required payment and selection concurrency tests.
- [ ] README documents how to run the second-round smoke chain.

## Known Deferrals After This Plan

- Real authentication and RBAC replace demo actor IDs.
- Registration and报名品类 constraints replace open order creation.
- Electronic signature replaces offline contract marking.
- Online payment callback and idempotency table replace manual payment creation.
- Refund and booth release task processing remain a later implementation round.
- Contract change and version evolution remain a later implementation round.
- Automatic scheduled timeout release can call the service added in Task 8.
- Full graphical booth map can build on the table-based selection page.
