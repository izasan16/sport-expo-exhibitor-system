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
