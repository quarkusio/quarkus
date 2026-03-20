---
name: quarkus
description: >-
  Core Quarkus development patterns, project structure, configuration, testing,
  and dev mode. Use this skill when working on any Quarkus application.
metadata:
  guide: "https://quarkus.io/guides/"
---

# Quarkus

> Quarkus is a Kubernetes-native Java framework tailored for GraalVM and HotSpot.
> Guide: https://quarkus.io/guides/

## Project Structure

- `src/main/java/` — application code.
- `src/main/resources/application.properties` — configuration.
- `src/main/resources/META-INF/resources/` — static web resources.
- `src/test/java/` — tests.

## Dev Mode

- Run with `./mvnw quarkus:dev` (Maven) or `./gradlew quarkusDev` (Gradle).
- **Hot reload**: code changes are picked up automatically on the next request — do NOT restart.
- **Continuous testing**: tests run automatically when code changes. Press `r` in the console to re-run.
- **Dev UI**: available at `/q/dev-ui` — browse extensions, configuration, and tools.
- **Dev Services**: backing services (databases, message brokers, etc.) are started automatically as containers.

## Configuration

- Use `application.properties` for configuration.
- Profile-specific config: prefix with `%dev.`, `%test.`, or `%prod.`.
- Environment variables: `QUARKUS_HTTP_PORT=9090` maps to `quarkus.http.port`.
- Use `@ConfigProperty(name = "my.prop")` to inject config values into beans.
- Use `@ConfigMapping` for type-safe config groups.

## CDI (Dependency Injection)

- Quarkus uses ArC, a build-time CDI implementation.
- Annotate classes with `@ApplicationScoped`, `@RequestScoped`, or `@Singleton` to make them CDI beans.
- Inject with `@Inject` on fields or constructors.
- Use `@Produces` methods for beans needing custom initialization.
- Beans are discovered at build time — unscoped classes are NOT beans.

## REST Endpoints

- Use Jakarta REST annotations: `@Path`, `@GET`, `@POST`, `@PUT`, `@DELETE`.
- Return POJOs — Jackson serialization is automatic with `quarkus-rest-jackson`.
- Use `@Valid` with Hibernate Validator for input validation.
- Inject services with `@Inject`.

## Testing

- Use `@QuarkusTest` for integration tests — starts the full application.
- Use REST Assured for HTTP endpoint testing (pre-configured with the test port).
- Use `@QuarkusComponentTest` for lightweight CDI-only tests.
- Use `@InjectMock` to mock CDI beans in tests.
- Use `@TestTransaction` to auto-rollback database changes.
- Dev Services provide test databases, message brokers, etc. automatically — no manual setup.
- ALWAYS write tests for every feature.

## Building

- `./mvnw package` — build a JAR (fast-jar format).
- `./mvnw package -Dnative` — build a native executable (requires GraalVM or container build).
- The app runs from `target/quarkus-app/quarkus-run.jar`.

## Common Pitfalls

- Do NOT use `new` to create CDI beans — always inject them.
- Do NOT set `quarkus.datasource.jdbc.url` without a profile prefix — this disables Dev Services.
- Do NOT restart dev mode after code changes — hot reload handles it.
- Do NOT skip writing tests — use `@QuarkusTest` for every feature.
- Prefer Quarkus extensions over manual library integration — extensions are optimized for build-time processing and native image.
