# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

`eduapp` (group `gr.aueb.cf`) is a Spring Boot 4.1 REST API written in Java 21, built with Gradle. It's an educational project (Coding Factory / AUEB) for managing teachers, users, and their personal info, with JWT-based stateless authentication and role/capability-based authorization.

## Commands

Use the Gradle wrapper (`./gradlew` on bash, `gradlew.bat` or `./gradlew` on PowerShell — both work here since PowerShell resolves `./gradlew` fine).

- Build: `./gradlew build`
- Run the app: `./gradlew bootRun`
- Run all tests: `./gradlew test`
- Run a single test class: `./gradlew test --tests "gr.aueb.cf.eduapp.EduAppApplicationTests"`
- Run a single test method: `./gradlew test --tests "gr.aueb.cf.eduapp.EduAppApplicationTests.someMethod"`

The `dev` Spring profile is active by default (`spring.profiles.active=dev` in `application.properties`), which loads `application-dev.properties`.

### Local environment

`application-dev.properties` imports `.env` (`spring.config.import=optional:file:.env[.properties]`) for these variables (see `.env.example`):

```
MYSQL_HOST, MYSQL_PORT, MYSQL_DB, MYSQL_USER, MYSQL_PASSWORD, JWT_SECRET_KEY
```

A local MySQL 8 instance is required to run the app or any test that touches the persistence layer — there's no embedded/test DB profile. `spring.jpa.hibernate.ddl-auto=validate` — schema is managed exclusively through Flyway migrations, not Hibernate auto-DDL.

## Architecture

### Layering

Standard layered structure, one package per concern under `gr.aueb.cf.eduapp`:

- `api` — `@RestController`s (thin; delegate to services, run request-level validators, translate results to `ResponseEntity`). Documented with springdoc/OpenAPI annotations (`@Operation`, `@ApiResponses`) — follow this pattern when adding endpoints, since Swagger UI (`/swagger-ui/**`) is the primary API reference.
- `service` — interface (`ITeacherService`) + implementation (`TeacherService`) pair per aggregate. Business logic, transactions (`@Transactional`), and method-level authorization (`@PreAuthorize`) live here, not in controllers.
- `model` — JPA entities. All extend `AbstractEntity` (`@MappedSuperclass`), which provides `createdAt`/`updatedAt` (via `@EnableJpaAuditing`) and a **soft-delete** convention: `deleted`/`deletedAt` fields plus a `softDelete()` method. Deletes throughout the app are soft deletes — repositories/services distinguish `findByX` (any) vs `findByXAndDeletedFalse` (active only).
- `dto` — Java records, split by direction/purpose: `*InsertDTO`, `*UpdateDTO`, `*ReadOnlyDTO`. Controllers never expose entities directly.
- `mapper` — a single `Mapper` component doing manual entity↔DTO conversion (no MapStruct).
- `repository` — Spring Data JPA repositories.
- `specification` / `core/filters` — dynamic query filtering (`TeacherSpecification` + `TeacherFilters`) used for the paginated/filtered teacher search endpoint.
- `validator` — manual `Validator`-style classes (e.g. `TeacherInsertValidator`) invoked explicitly by controllers before service calls, populating a `BindingResult` that's wrapped into a `ValidationException` on failure.
- `core/exceptions` + `core/ErrorHandler` — a `@RestControllerAdvice` centralizes all exception→HTTP mapping. Each domain exception (`EntityNotFoundException`, `EntityAlreadyExistsException`, `EntityInvalidArgumentException`, `ValidationException`, `FileUploadException`) carries a business `code` and maps to a specific HTTP status; add new exceptions here rather than handling errors ad hoc in controllers/services.
- `authentication` / `security` — JWT issuance/validation (`JwtService`), the auth filter chain (`JwtAuthenticationFilter`, `SecurityConfiguration`), `CustomUserDetailsService`, and `SecurityService` (used from `@PreAuthorize` SpEL expressions, e.g. ownership checks like `isOwnTeacherProfile`).

### Entity relationships (core domain)

- `User` (implements `UserDetails`) ←→ `Teacher`: one-to-one, owned by `Teacher` (`user_id` FK). `User.getAuthorities()` derives Spring Security authorities from `Role` (`ROLE_<name>`) plus each `Capability` name on that role.
- `Teacher` has a `Region` (many-to-one) and a `PersonalInfo` (one-to-one, `CascadeType.ALL` + `orphanRemoval`, unidirectional — always navigate from `Teacher`, never the reverse).
- `PersonalInfo` holds an `Attachment` (the AMKA document file metadata: filename, saved name, path, content type).
- `Role` ←→ `Capability`: many-to-many, driving authorization. Authorities used in `@PreAuthorize`/`SecurityConfiguration` (e.g. `VIEW_TEACHER`, `EDIT_TEACHER`, `DELETE_TEACHER`, `VIEW_TEACHERS`, `VIEW_ONLY_TEACHER`, `VIEW_USER`) come from the `capabilities` table seeded by Flyway migrations, not from an enum in code — check `src/main/resources/db/migration/` when adding a new authority.

### Authorization model

Two layers work together and must stay in sync when adding endpoints:
1. **URL-level** rules in `SecurityConfiguration.securityFilterChain` (coarse, method+path based).
2. **Method-level** `@PreAuthorize` on service methods (fine-grained, can reference custom beans like `@securityService.isOwnTeacherProfile(...)`).

Auth is fully stateless (`SessionCreationPolicy.STATELESS`): a `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter` and populates the `SecurityContext` from the bearer token on every request. There are no server-side sessions or CSRF tokens (CSRF is explicitly disabled — this is a CSR/SPA backend, not server-rendered).

### File uploads

Multipart uploads (e.g. the teacher AMKA file) are validated/typed with Apache Tika, saved under `file.upload.dir` (`uploads/`, git-ignored), and use `TransactionSynchronizationManager.registerSynchronization(...).afterCommit()` so the file is written to disk only after the DB transaction commits (avoiding orphaned files on rollback). `saveAmkaFile` is also `@Retryable` (Spring's resilience support, `@EnableResilientMethods`) for transient I/O/HTTP failures.

### Database migrations

Flyway-managed, versioned SQL files in `src/main/resources/db/migration/` (`V1__initial_schema.sql`, `V2__insert_regions.sql`, `V3__insert_roles_capabilites.sql`, `V4__insert_view_user_capability_to_admin.sql`). Add new schema/data changes as new `V{n}__description.sql` files — never edit an already-applied migration.

## Notes

- `.env` is git-ignored and must never be committed; `.env.example` documents the required keys.
- `uploads/` and `logs/` are runtime output directories (not currently git-ignored, so check `git status` before staging) — the app writes application logs to `logs/eduapp.log` and uploaded files under `uploads/`.
