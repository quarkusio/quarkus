{#include readme-header /}

A starter Flyway MongoDB migration is provided at `src/main/resources/db/migration/V1.0.0__init.js`.

## Prerequisites

The Flyway MongoDB extension executes migration scripts via the MongoDB shell. Install
[`mongosh`](https://www.mongodb.com/docs/mongodb-shell/install/) and make sure it is on
`PATH` of any process that runs the application (locally, in CI, and in your container image).

## Configuration

Configure the MongoDB connection and the target database in `src/main/resources/application.properties`:

```properties
quarkus.mongodb.connection-string=mongodb://localhost:27017
quarkus.mongodb.database=mydb

quarkus.flyway-mongodb.database=mydb
quarkus.flyway-mongodb.migrate-at-start=true
```

See the [Flyway MongoDB guide](https://quarkus.io/guides/flyway-mongodb) for the complete
list of configuration properties.
