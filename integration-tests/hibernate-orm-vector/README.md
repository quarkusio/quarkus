# Hibernate ORM Vector Integration Tests

Integration tests for `hibernate-vector` support in Quarkus.

`hibernate-vector` is a Hibernate ORM module that enables vector similarity search
via database-native vector types (e.g. pgvector for PostgreSQL). It is commonly used
in AI/ML applications to persist and query embeddings.

## Running the tests

Tests require a PostgreSQL instance with the [pgvector](https://github.com/pgvector/pgvector)
extension installed. By default, tests are skipped unless the `test-containers` property is set.

```bash
./mvnw verify -pl integration-tests/hibernate-orm-vector -Dtest-containers
```

The test uses Quarkus Dev Services with the `pgvector/pgvector:pg18` Docker image,
which bundles PostgreSQL and the pgvector extension together.
