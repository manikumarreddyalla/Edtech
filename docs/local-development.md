# Local Development

## Prerequisites

- JDK 21 or newer.
- Maven 3.9+.
- Node.js 22+ and npm.
- Docker Compose for the local PostgreSQL service.

## Configure local variables

Copy `.env.example` to `.env` and replace the placeholder database and bootstrap-admin values. Generate a dedicated random JWT key (do not use the test key or commit `.env`). For PowerShell:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Set the resulting value as `JWT_SECRET_BASE64` in `.env`. The API reads process environment variables; when starting it from PowerShell, load the values from `.env` into that process environment or set them explicitly. Never use the example credentials outside local development.

## Start services

```powershell
docker compose up -d postgres
cd backend
mvn spring-boot:run
```

In another terminal:

```powershell
cd frontend
npm ci
npm run dev
```

The API listens on `http://localhost:8080`, the frontend on `http://localhost:5173`, and OpenAPI UI is at `http://localhost:8080/swagger-ui.html`. Flyway applies the schema during API startup. The `dev` profile provisions an institution with code `DEMO` and the configured bootstrap administrator on first start. The bootstrap password must be at least 12 characters.

## Checks

```powershell
cd backend
mvn test
cd ..\frontend
npm test
npm run typecheck
npm run build
```

The backend test profile uses an isolated H2 database and does not create demo users. The PostgreSQL migration is also intended to run on a clean local PostgreSQL database.
