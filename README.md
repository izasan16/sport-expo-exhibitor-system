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

Backend compile:

```bash
cd backend
mvn test -DskipTests
```

Backend tests with an external PostgreSQL instance:

```bash
cd backend
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/database \
SPRING_DATASOURCE_USERNAME=exhibitor \
SPRING_DATASOURCE_PASSWORD=exhibitor \
APP_DB_SCHEMA=exhibitor_sys_test \
mvn test
```

Backend Testcontainers tests, used when no external datasource is configured and requires Docker:

```bash
cd backend
mvn test
```

Frontend build:

```bash
cd frontend
npm run build
```

## Second-round business smoke flow

Demo actor IDs used before real authentication lands:

```text
DEMO_EXHIBITOR_ID=00000000-0000-0000-0000-000000000001
DEMO_ADMIN_ID=00000000-0000-0000-0000-000000000002
```

The application initializes tables only for an empty schema. If an existing local
`exhibitor_sys` schema was created from an older baseline, reset it before the
smoke run:

```bash
psql -h localhost -p 18182 -U postgres -d postgres -c "drop schema if exists exhibitor_sys cascade"
```

Start backend and frontend, then verify this UI chain:

```text
1. Admin creates an event from 展会管理.
2. Admin creates two 9.00 STANDARD booths from 展位管理.
3. Admin creates an 18.00 area tier from 面积档位.
4. Admin creates a STANDARD price rule from 价格规则.
5. Exhibitor creates an order from 我要下单.
6. Admin sales-confirms the order from 订单管理.
7. Admin marks the generated contract as offline signed from 合同管理.
8. Exhibitor submits an offline payment from 提交付款.
9. Admin confirms the payment from 付款确认.
10. Exhibitor selects the two 9.00 STANDARD booths from 选择展位.
11. Admin approves the booth selection from 选位审核.
```

Expected final state:

```text
order.status=PAID
selectionEligibility.status=CLOSED
boothSelection.status=CONFIRMED
selected booths status=CONFIRMED
```
