# Quarkus Workspace Dependency Graph Overview

## High-Level Module Structure

Based on the Maven reactor build order and module structure, here's the dependency flow:

```
Root Project (quarkus-project)
├── Independent Projects
│   ├── parent (foundation POM)
│   ├── ide-config (IDE tools)
│   ├── revapi (API compatibility)
│   ├── enforcer-rules (build rules)
│   ├── arc (CDI container)
│   ├── bootstrap (app bootstrapping)
│   ├── qute (template engine)
│   ├── tools (dev utilities)
│   ├── vertx-utils (Vert.x utilities)
│   ├── resteasy-reactive (REST framework)
│   └── extension-maven-plugin (extension tooling)
│
├── BOMs & Build
│   ├── bom/application (app dependencies)
│   ├── bom/test (test dependencies)
│   ├── bom/dev-ui (dev UI dependencies)
│   └── build-parent (build configuration)
│
├── Core
│   ├── builder (core building)
│   ├── class-change-agent (hot reload)
│   ├── deployment (build-time processing)
│   ├── devmode-spi (dev mode interfaces)
│   ├── junit4-mock (testing support)
│   ├── launcher (app launching)
│   ├── processor (annotation processing)
│   └── runtime (runtime components)
│
├── Extensions (130+ modules)
│   ├── Web Framework Extensions
│   │   ├── resteasy-classic
│   │   ├── resteasy-reactive
│   │   ├── vertx-http
│   │   ├── websockets
│   │   └── grpc
│   │
│   ├── Data Persistence
│   │   ├── hibernate-orm
│   │   ├── hibernate-reactive
│   │   ├── mongodb-client
│   │   ├── redis-client
│   │   └── agroal (datasource)
│   │
│   ├── Security Extensions
│   │   ├── elytron-security
│   │   ├── oidc
│   │   ├── security-webauthn
│   │   └── keycloak-authorization
│   │
│   ├── Cloud & DevOps
│   │   ├── kubernetes
│   │   ├── openshift-client
│   │   ├── container-image
│   │   └── observability-devservices
│   │
│   └── Integration Extensions
│       ├── kafka-client
│       ├── messaging (SmallRye)
│       ├── opentelemetry
│       └── micrometer
│
├── DevTools
│   ├── cli (command line interface)
│   ├── maven (Maven integration)
│   ├── gradle (Gradle integration)
│   └── config-doc-maven-plugin
│
├── Test Framework
│   └── Various testing utilities
│
├── Integration Tests (100+ test modules)
│   └── End-to-end testing scenarios
│
└── TCKs (Technology Compatibility Kits)
    ├── jakarta-cdi
    ├── microprofile-config
    └── microprofile-context-propagation
```

## Key Dependency Relationships

1. **Foundation Layer**: Independent projects provide core functionality
2. **Core Layer**: Depends on independent projects, provides runtime/deployment split
3. **Extensions Layer**: Each extension typically has runtime + deployment modules
4. **Integration Layer**: DevTools, tests, and TCKs depend on core + extensions

## Build Order Dependencies

The Maven reactor shows these key dependency chains:

1. `parent` → `arc` → `bootstrap` → `qute` → `tools`
2. `core/runtime` → `core/deployment` → extensions
3. Extensions are built in dependency order (e.g., `vertx` before `vertx-http`)
4. Integration tests are built after all extensions

This is a multi-module Maven workspace with ~300+ modules following a layered architecture pattern.