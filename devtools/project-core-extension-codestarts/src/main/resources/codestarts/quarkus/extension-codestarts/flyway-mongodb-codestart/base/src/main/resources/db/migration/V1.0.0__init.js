// Example Flyway MongoDB migration. Edit or replace with your own script.
// Each script is executed by Flyway via the `mongosh` shell against the
// database configured by `quarkus.flyway-mongodb.database` (or the matching
// `quarkus.mongodb.<client>.database`).
db.createCollection("greetings");
db.greetings.insertOne({ message: "Hello from Quarkus Flyway MongoDB!" });
