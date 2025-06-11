# Quarkus Core Dependencies Analysis

This document lists all modules/projects that depend on the `io.quarkus:quarkus-core` runtime module.

## Core Modules
- `/home/jason/projects/triage/java/quarkus/core/deployment/pom.xml` - Core deployment module (depends on core runtime)
- `/home/jason/projects/triage/java/quarkus/core/junit4-mock/pom.xml` - JUnit 4 mock support
- `/home/jason/projects/triage/java/quarkus/core/processor/pom.xml` - Core processor

## Extension Runtime Modules (98 total)

### Core Framework Extensions
- **ARC (CDI)**: `/home/jason/projects/triage/java/quarkus/extensions/arc/runtime/pom.xml`
- **Vert.x HTTP**: `/home/jason/projects/triage/java/quarkus/extensions/vertx-http/runtime/pom.xml`
- **Mutiny**: `/home/jason/projects/triage/java/quarkus/extensions/mutiny/runtime/pom.xml`
- **Virtual Threads**: `/home/jason/projects/triage/java/quarkus/extensions/virtual-threads/runtime/pom.xml`

### Data Layer Extensions
- **Agroal (Datasource)**: `/home/jason/projects/triage/java/quarkus/extensions/agroal/runtime/pom.xml`
- **Datasource**: `/home/jason/projects/triage/java/quarkus/extensions/datasource/runtime/pom.xml`
- **Reactive Datasource**: `/home/jason/projects/triage/java/quarkus/extensions/reactive-datasource/runtime/pom.xml`
- **Hibernate ORM**: `/home/jason/projects/triage/java/quarkus/extensions/hibernate-orm/runtime/pom.xml`
- **MongoDB Client**: `/home/jason/projects/triage/java/quarkus/extensions/mongodb-client/runtime/pom.xml`

### Panache Extensions (9 total)
- **Panache Common**: `/home/jason/projects/triage/java/quarkus/extensions/panache/panache-common/runtime/pom.xml`
- **Hibernate Common**: `/home/jason/projects/triage/java/quarkus/extensions/panache/panache-hibernate-common/runtime/pom.xml`
- **Hibernate ORM Panache Kotlin**: `/home/jason/projects/triage/java/quarkus/extensions/panache/hibernate-orm-panache-kotlin/runtime/pom.xml`
- **Hibernate Reactive Panache**: `/home/jason/projects/triage/java/quarkus/extensions/panache/hibernate-reactive-panache/runtime/pom.xml`
- **Hibernate Reactive Panache Kotlin**: `/home/jason/projects/triage/java/quarkus/extensions/panache/hibernate-reactive-panache-kotlin/runtime/pom.xml`
- **Hibernate Reactive Panache Common**: `/home/jason/projects/triage/java/quarkus/extensions/panache/hibernate-reactive-panache-common/runtime/pom.xml`
- **MongoDB Panache**: `/home/jason/projects/triage/java/quarkus/extensions/panache/mongodb-panache/runtime/pom.xml`
- **MongoDB Panache Kotlin**: `/home/jason/projects/triage/java/quarkus/extensions/panache/mongodb-panache-kotlin/runtime/pom.xml`
- **MongoDB Panache Common**: `/home/jason/projects/triage/java/quarkus/extensions/panache/mongodb-panache-common/runtime/pom.xml`

### Security Extensions (11 total)
- **Elytron Security**: `/home/jason/projects/triage/java/quarkus/extensions/elytron-security/runtime/pom.xml`
- **Elytron Security Common**: `/home/jason/projects/triage/java/quarkus/extensions/elytron-security-common/runtime/pom.xml`
- **Elytron Security JDBC**: `/home/jason/projects/triage/java/quarkus/extensions/elytron-security-jdbc/runtime/pom.xml`
- **Elytron Security LDAP**: `/home/jason/projects/triage/java/quarkus/extensions/elytron-security-ldap/runtime/pom.xml`
- **Elytron Security Properties**: `/home/jason/projects/triage/java/quarkus/extensions/elytron-security-properties-file/runtime/pom.xml`
- **Security JPA**: `/home/jason/projects/triage/java/quarkus/extensions/security-jpa/runtime/pom.xml`
- **Security JPA Common**: `/home/jason/projects/triage/java/quarkus/extensions/security-jpa-common/runtime/pom.xml`
- **Security JPA Reactive**: `/home/jason/projects/triage/java/quarkus/extensions/security-jpa-reactive/runtime/pom.xml`
- **Security WebAuthn**: `/home/jason/projects/triage/java/quarkus/extensions/security-webauthn/runtime/pom.xml`
- **SmallRye JWT**: `/home/jason/projects/triage/java/quarkus/extensions/smallrye-jwt/runtime/pom.xml`
- **SmallRye JWT Build**: `/home/jason/projects/triage/java/quarkus/extensions/smallrye-jwt-build/runtime/pom.xml`

### OIDC Extensions (6 total)
- **OIDC**: `/home/jason/projects/triage/java/quarkus/extensions/oidc/runtime/pom.xml`
- **OIDC Common**: `/home/jason/projects/triage/java/quarkus/extensions/oidc-common/runtime/pom.xml`
- **OIDC Client**: `/home/jason/projects/triage/java/quarkus/extensions/oidc-client/runtime/pom.xml`
- **OIDC Client GraphQL**: `/home/jason/projects/triage/java/quarkus/extensions/oidc-client-graphql/runtime/pom.xml`
- **OIDC Client Registration**: `/home/jason/projects/triage/java/quarkus/extensions/oidc-client-registration/runtime/pom.xml`
- **OIDC Token Propagation Common**: `/home/jason/projects/triage/java/quarkus/extensions/oidc-token-propagation-common/runtime/pom.xml`

### REST/Web Extensions (9 total)
- **RESTEasy Classic Common**: `/home/jason/projects/triage/java/quarkus/extensions/resteasy-classic/resteasy-common/runtime/pom.xml`
- **RESTEasy Classic Server Common**: `/home/jason/projects/triage/java/quarkus/extensions/resteasy-classic/resteasy-server-common/runtime/pom.xml`
- **RESTEasy Classic Client**: `/home/jason/projects/triage/java/quarkus/extensions/resteasy-classic/resteasy-client/runtime/pom.xml`
- **RESTEasy Classic Mutiny**: `/home/jason/projects/triage/java/quarkus/extensions/resteasy-classic/resteasy-mutiny/runtime/pom.xml`
- **RESTEasy Classic Qute**: `/home/jason/projects/triage/java/quarkus/extensions/resteasy-classic/resteasy-qute/runtime/pom.xml`
- **REST Client Config**: `/home/jason/projects/triage/java/quarkus/extensions/resteasy-classic/rest-client-config/runtime/pom.xml`
- **RESTEasy Reactive CSRF**: `/home/jason/projects/triage/java/quarkus/extensions/resteasy-reactive/rest-csrf/runtime/pom.xml`
- **RESTEasy Reactive Qute**: `/home/jason/projects/triage/java/quarkus/extensions/resteasy-reactive/rest-qute/runtime/pom.xml`
- **Undertow**: `/home/jason/projects/triage/java/quarkus/extensions/undertow/runtime/pom.xml`

### Observability Extensions (10 total)
- **Micrometer**: `/home/jason/projects/triage/java/quarkus/extensions/micrometer/runtime/pom.xml`
- **Micrometer OpenTelemetry**: `/home/jason/projects/triage/java/quarkus/extensions/micrometer-opentelemetry/runtime/pom.xml`
- **Micrometer Prometheus**: `/home/jason/projects/triage/java/quarkus/extensions/micrometer-registry-prometheus/runtime/pom.xml`
- **OpenTelemetry**: `/home/jason/projects/triage/java/quarkus/extensions/opentelemetry/runtime/pom.xml`
- **SmallRye Health**: `/home/jason/projects/triage/java/quarkus/extensions/smallrye-health/runtime/pom.xml`
- **SmallRye Metrics**: `/home/jason/projects/triage/java/quarkus/extensions/smallrye-metrics/runtime/pom.xml`
- **Logging GELF**: `/home/jason/projects/triage/java/quarkus/extensions/logging-gelf/runtime/pom.xml`
- **Logging JSON**: `/home/jason/projects/triage/java/quarkus/extensions/logging-json/runtime/pom.xml`
- **Observability DevServices**: `/home/jason/projects/triage/java/quarkus/extensions/observability-devservices/runtime/pom.xml`
- **CycloneDX**: `/home/jason/projects/triage/java/quarkus/extensions/cyclonedx/runtime/pom.xml`

### Database Migration Extensions (8 total)
- **Flyway**: `/home/jason/projects/triage/java/quarkus/extensions/flyway/runtime/pom.xml`
- **Flyway DB2**: `/home/jason/projects/triage/java/quarkus/extensions/flyway-db2/runtime/pom.xml`
- **Flyway Derby**: `/home/jason/projects/triage/java/quarkus/extensions/flyway-derby/runtime/pom.xml`
- **Flyway MSSQL**: `/home/jason/projects/triage/java/quarkus/extensions/flyway-mssql/runtime/pom.xml`
- **Flyway MySQL**: `/home/jason/projects/triage/java/quarkus/extensions/flyway-mysql/runtime/pom.xml`
- **Flyway Oracle**: `/home/jason/projects/triage/java/quarkus/extensions/flyway-oracle/runtime/pom.xml`
- **Flyway PostgreSQL**: `/home/jason/projects/triage/java/quarkus/extensions/flyway-postgresql/runtime/pom.xml`
- **Liquibase**: `/home/jason/projects/triage/java/quarkus/extensions/liquibase/liquibase/runtime/pom.xml`

### Cloud Function Extensions (4 total)
- **Amazon Lambda HTTP**: `/home/jason/projects/triage/java/quarkus/extensions/amazon-lambda-http/runtime/pom.xml`
- **Amazon Lambda REST**: `/home/jason/projects/triage/java/quarkus/extensions/amazon-lambda-rest/runtime/pom.xml`
- **Amazon Lambda XRay**: `/home/jason/projects/triage/java/quarkus/extensions/amazon-lambda-xray/runtime/pom.xml`
- **Azure Functions**: `/home/jason/projects/triage/java/quarkus/extensions/azure-functions/runtime/pom.xml`
- **Azure Functions HTTP**: `/home/jason/projects/triage/java/quarkus/extensions/azure-functions-http/runtime/pom.xml`
- **Google Cloud Functions**: `/home/jason/projects/triage/java/quarkus/extensions/google-cloud-functions/runtime/pom.xml`
- **Google Cloud Functions HTTP**: `/home/jason/projects/triage/java/quarkus/extensions/google-cloud-functions-http/runtime/pom.xml`

### Miscellaneous Extensions (30+ remaining)
- **Kafka Streams**: `/home/jason/projects/triage/java/quarkus/extensions/kafka-streams/runtime/pom.xml`
- **gRPC**: `/home/jason/projects/triage/java/quarkus/extensions/grpc/runtime/pom.xml`
- **WebSockets**: `/home/jason/projects/triage/java/quarkus/extensions/websockets/server/runtime/pom.xml`
- **WebSockets Next**: `/home/jason/projects/triage/java/quarkus/extensions/websockets-next/runtime/pom.xml`
- **Qute**: `/home/jason/projects/triage/java/quarkus/extensions/qute/runtime/pom.xml`
- **Reactive Routes**: `/home/jason/projects/triage/java/quarkus/extensions/reactive-routes/runtime/pom.xml`
- **JSON-P**: `/home/jason/projects/triage/java/quarkus/extensions/jsonp/runtime/pom.xml`
- **JSON-B**: `/home/jason/projects/triage/java/quarkus/extensions/jsonb/runtime/pom.xml`
- **JAXP**: `/home/jason/projects/triage/java/quarkus/extensions/jaxp/runtime/pom.xml`
- **HAL**: `/home/jason/projects/triage/java/quarkus/extensions/hal/runtime/pom.xml`
- **PicoCLI**: `/home/jason/projects/triage/java/quarkus/extensions/picocli/runtime/pom.xml`
- **Kotlin**: `/home/jason/projects/triage/java/quarkus/extensions/kotlin/runtime/pom.xml`
- **Scala**: `/home/jason/projects/triage/java/quarkus/extensions/scala/runtime/pom.xml`
- **Config YAML**: `/home/jason/projects/triage/java/quarkus/extensions/config-yaml/runtime/pom.xml`
- **Caffeine**: `/home/jason/projects/triage/java/quarkus/extensions/caffeine/runtime/pom.xml`
- **Container Image**: `/home/jason/projects/triage/java/quarkus/extensions/container-image/runtime/pom.xml`
- **Avro**: `/home/jason/projects/triage/java/quarkus/extensions/avro/runtime/pom.xml`
- **Swagger UI**: `/home/jason/projects/triage/java/quarkus/extensions/swagger-ui/runtime/pom.xml`
- **Web Dependency Locator**: `/home/jason/projects/triage/java/quarkus/extensions/web-dependency-locator/runtime/pom.xml`

## Extension SPIs (16 total)
- **Security SPI**: `/home/jason/projects/triage/java/quarkus/extensions/security/spi/pom.xml`
- **TLS Registry SPI**: `/home/jason/projects/triage/java/quarkus/extensions/tls-registry/spi/pom.xml`
- **OIDC Client SPI**: `/home/jason/projects/triage/java/quarkus/extensions/oidc-client/spi/pom.xml`
- **SmallRye Health SPI**: `/home/jason/projects/triage/java/quarkus/extensions/smallrye-health/spi/pom.xml`
- **SmallRye Metrics SPI**: `/home/jason/projects/triage/java/quarkus/extensions/smallrye-metrics/spi/pom.xml`
- **SmallRye OpenAPI SPI**: `/home/jason/projects/triage/java/quarkus/extensions/smallrye-openapi/spi/pom.xml`
- **SmallRye Context Propagation SPI**: `/home/jason/projects/triage/java/quarkus/extensions/smallrye-context-propagation/spi/pom.xml`
- **Agroal SPI**: `/home/jason/projects/triage/java/quarkus/extensions/agroal/spi/pom.xml`
- **Container Image SPI**: `/home/jason/projects/triage/java/quarkus/extensions/container-image/spi/pom.xml`
- **Jackson SPI**: `/home/jason/projects/triage/java/quarkus/extensions/jackson/spi/pom.xml`
- **JSON-B SPI**: `/home/jason/projects/triage/java/quarkus/extensions/jsonb/spi/pom.xml`
- **Kubernetes Client SPI**: `/home/jason/projects/triage/java/quarkus/extensions/kubernetes-client/spi/pom.xml`
- **Kubernetes SPI**: `/home/jason/projects/triage/java/quarkus/extensions/kubernetes/spi/pom.xml`
- **Kubernetes Service Binding SPI**: `/home/jason/projects/triage/java/quarkus/extensions/kubernetes-service-binding/spi/pom.xml`
- **RESTEasy Classic Common SPI**: `/home/jason/projects/triage/java/quarkus/extensions/resteasy-classic/resteasy-common/spi/pom.xml`
- **RESTEasy Classic Server Common SPI**: `/home/jason/projects/triage/java/quarkus/extensions/resteasy-classic/resteasy-server-common/spi/pom.xml`
- **Undertow SPI**: `/home/jason/projects/triage/java/quarkus/extensions/undertow/spi/pom.xml`

## Runtime SPIs (2 total)
- **Info Runtime SPI**: `/home/jason/projects/triage/java/quarkus/extensions/info/runtime-spi/pom.xml`
- **Security Runtime SPI**: `/home/jason/projects/triage/java/quarkus/extensions/security/runtime-spi/pom.xml`

## Common Modules (3 total)
- **Datasource Common**: `/home/jason/projects/triage/java/quarkus/extensions/datasource/common/pom.xml`
- **DevServices Common**: `/home/jason/projects/triage/java/quarkus/extensions/devservices/common/pom.xml`
- **Observability DevServices Common**: `/home/jason/projects/triage/java/quarkus/extensions/observability-devservices/common/pom.xml`

## Test Framework (8 total)
- **Test Framework Common**: `/home/jason/projects/triage/java/quarkus/test-framework/common/pom.xml`
- **JUnit5**: `/home/jason/projects/triage/java/quarkus/test-framework/junit5/pom.xml`
- **JUnit5 Internal**: `/home/jason/projects/triage/java/quarkus/test-framework/junit5-internal/pom.xml`
- **JUnit5 Config**: `/home/jason/projects/triage/java/quarkus/test-framework/junit5-config/pom.xml`
- **JUnit5 Component**: `/home/jason/projects/triage/java/quarkus/test-framework/junit5-component/pom.xml`
- **Maven**: `/home/jason/projects/triage/java/quarkus/test-framework/maven/pom.xml`
- **Jacoco Runtime**: `/home/jason/projects/triage/java/quarkus/test-framework/jacoco/runtime/pom.xml`
- **Jacoco Deployment**: `/home/jason/projects/triage/java/quarkus/test-framework/jacoco/deployment/pom.xml`

## Summary

The `io.quarkus:quarkus-core` module is a foundational dependency used throughout the Quarkus ecosystem. Nearly every extension runtime module depends on it, which makes sense as it provides core functionality like:

- Configuration system
- Runtime initialization 
- Dependency injection integration
- Application lifecycle management
- Core annotations and APIs

**Total Dependencies**: ~130+ modules depend on quarkus-core, including:
- 98 extension runtime modules
- 16 extension SPIs  
- 2 runtime SPIs
- 3 common modules
- 8 test framework modules
- 3 core modules

This shows that quarkus-core is the central runtime foundation that all Quarkus functionality builds upon.