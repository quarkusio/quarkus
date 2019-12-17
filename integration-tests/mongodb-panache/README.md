# MongoDB With Panache Integration Test

You can launch the integration test via the standard Maven command

```
mvn clean test
```

If you want to launch them in native, you need to use the native profile, you can do it with the following Maven command

```
mvn clean integration-test -Dnative
```

There is a `/persons/entity/transaction` and a `/persons/repository/transaction` endpoints to test the transaction support
but as it needs a MongoDB replica set there is no automated test on it.

If you need to test transaction support, you can follow these steps :
- Launch a MongoDB cluster, you can do it thanks to the docker compose file in `src/test/resources/docker-compose.yml`
- Launch the mongo shell in one of the MongoDB nodes and setup a replica set:
    - `docker exec -ti <container> mongo`
    - Launch the command that you can find inside `src/test/resources/rsConfig.js
- Configure the `quarkus.mongodb.connection-string` property to target the port of the **primary** node
