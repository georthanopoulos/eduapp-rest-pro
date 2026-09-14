# eduapp-rest-pro

A Spring Boot REST API for managing teachers, their personal info, and user accounts, with JWT-based authentication and role/capability-based authorization. Built as an educational project (Coding Factory / AUEB).

## Tech stack

- Java 21
- Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation)
- MySQL 8 + Flyway migrations
- JWT (`jjwt`) for stateless authentication
- springdoc-openapi (Swagger UI)
- Apache Tika (file type detection for uploads)
- Lombok
- Gradle

## Prerequisites

- JDK 21
- A running MySQL 8 instance
- Gradle Wrapper (bundled — no local Gradle install needed)

## Setup

1. Copy `.env.example` to `.env` and fill in your local values:

   ```
   MYSQL_HOST=
   MYSQL_PORT=
   MYSQL_DB=
   MYSQL_USER=
   MYSQL_PASSWORD=
   JWT_SECRET_KEY=
   ```

   `JWT_SECRET_KEY` must be a Base64-encoded string suitable for an HMAC-SHA256 key.

   `.env` is loaded automatically via `spring.config.import` and must never be committed.

2. Create the target MySQL database (matching `MYSQL_DB`). Schema and seed data are applied automatically by Flyway on startup — no manual DDL required.

3. Run the app:

   ```bash
   ./gradlew bootRun
   ```

   The `dev` Spring profile is active by default and loads `application-dev.properties`.

## API documentation

Once running, Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON is served at `/v3/api-docs`.

## Authentication

Authentication is stateless (JWT bearer tokens, no server-side sessions):

1. `POST /api/v1/auth/authenticate` with a username/password to receive a JWT.
2. Send the token as `Authorization: Bearer <token>` on subsequent requests.

Access to endpoints is controlled both at the URL level (`SecurityConfiguration`) and at the method level via capability-based `@PreAuthorize` checks (e.g. `VIEW_TEACHER`, `EDIT_TEACHER`, `DELETE_TEACHER`, `VIEW_TEACHERS`).

## Key endpoints

| Method | Path                              | Description                                  |
|--------|-----------------------------------|-----------------------------------------------|
| POST   | `/api/v1/auth/authenticate`       | Authenticate and receive a JWT                |
| POST   | `/api/v1/teachers`                | Register a new teacher                        |
| GET    | `/api/v1/teachers`                | Paginated, filtered list of teachers          |
| GET    | `/api/v1/teachers/{uuid}`         | Get a teacher by UUID                         |
| PUT    | `/api/v1/teachers/{uuid}`         | Update a teacher                              |
| DELETE | `/api/v1/teachers/{uuid}`         | Soft-delete a teacher                         |
| POST   | `/api/v1/teachers/{uuid}/amka-file` | Upload/replace a teacher's AMKA attachment  |

See Swagger UI for full request/response schemas and error responses.

## Building and testing

```bash
./gradlew build          # build the project
./gradlew test           # run all tests
./gradlew test --tests "gr.aueb.cf.eduapp.EduAppApplicationTests"   # run a single test class
```

Tests that touch persistence require a reachable MySQL instance and a valid `.env` — there is no embedded/in-memory test profile.

## Project structure

```
src/main/java/gr/aueb/cf/eduapp/
├── api/            REST controllers
├── authentication/ JWT issuance/validation, user details, auth service
├── core/           error handling, exceptions, request filters, OpenAPI config
├── dto/            request/response records
├── mapper/         entity <-> DTO conversion
├── model/          JPA entities
├── repository/     Spring Data JPA repositories
├── security/       Spring Security configuration and filters
├── service/        business logic
├── specification/  JPA Specifications for dynamic filtering
└── validator/       request validators

src/main/resources/db/migration/   Flyway SQL migrations
```

For more detail on the architecture and conventions, see [CLAUDE.md](CLAUDE.md).
