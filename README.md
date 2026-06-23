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

## Render Deployment

The repository includes `render.yaml` for a Render Blueprint deployment:

- `sport-expo-exhibitor-web`: static React site
- `sport-expo-exhibitor-api`: Dockerized Spring Boot API
- `sport-expo-exhibitor-db`: Render PostgreSQL
- `sport-expo-exhibitor-cache`: Render Key Value cache

Deploy steps:

```text
1. Push the repository to GitHub.
2. In Render, choose New -> Blueprint.
3. Select this repository and keep the Blueprint file path as render.yaml.
4. Deploy the Blueprint.
5. After creation, verify the actual service URLs.
```

Default Blueprint URLs are:

```text
Frontend: https://sport-expo-exhibitor-web.onrender.com
Backend:  https://sport-expo-exhibitor-api.onrender.com
Health:   https://sport-expo-exhibitor-api.onrender.com/api/health
```

If Render changes the service slug because of a name collision, update:

```text
Frontend env VITE_API_BASE_URL -> actual backend URL
Backend env APP_CORS_ALLOWED_ORIGINS -> actual frontend URL
```

Render free instances can sleep after inactivity. The first customer visit after
sleep may take longer while the backend starts again.

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

## Productized UI routes

Exhibitor:

- `/exhibitor` - home dashboard and next action
- `/exhibitor/orders/new` - create participation order
- `/exhibitor/orders` - order list
- `/exhibitor/orders/{orderId}` - order detail
- `/exhibitor/orders/{orderId}/payment` - submit payment for one order
- `/exhibitor/orders/{orderId}/selection` - select booths for one order

Admin:

- `/admin/dashboard` - pending work dashboard
- `/admin/users` - user management
- `/admin/roles` - role management
- `/admin/permissions` - permission resources
- `/admin/role-permissions` - role permission matrix
- `/admin/orders` - order review list
- `/admin/orders/{orderId}` - order detail
- `/admin/payments` - payment confirmation
- `/admin/selections` - booth selection review

## RBAC smoke flow

Demo accounts:

| Username | Password | Expected landing |
| --- | --- | --- |
| `admin` | `demo123` | `/admin/dashboard` |
| `organizer` | `demo123` | `/admin/dashboard` |
| `sales01` | `demo123` | `/admin/dashboard` |
| `finance01` | `demo123` | `/admin/dashboard` |
| `service01` | `demo123` | `/admin/dashboard` |
| `exhibitor01` | `demo123` | `/exhibitor` |

Verify this flow after starting backend and frontend:

```text
1. Open /login and sign in with admin.
2. Confirm the admin shell shows 用户管理 / 角色管理 / 权限资源 / 授权矩阵.
3. Open /admin/users and confirm friendly user type labels and exhibitor names are shown.
4. Open /admin/roles and verify create / copy / status actions render under role permissions.
5. Open /admin/permissions and verify the resource tree and table load together.
6. Open /admin/role-permissions and verify selecting a role loads its permission tree.
7. Sign out and sign in with exhibitor01, then confirm landing on /exhibitor.
8. For a multi-role account, switch workbenches from the shell without logging out.
9. Visit a page without page permission and confirm the app lands on /403.
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
