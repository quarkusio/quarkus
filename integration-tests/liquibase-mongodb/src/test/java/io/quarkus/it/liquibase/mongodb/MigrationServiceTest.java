package io.quarkus.it.liquibase.mongodb;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import io.quarkus.mongodb.MongoClientName;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import liquibase.exception.LiquibaseException;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, initArgs = @ResourceArg(name = "port", value = "27020"))
@DisabledOnOs(OS.WINDOWS)
class MigrationServiceTest {

    @Inject
    @MongoClientName("inventory")
    MongoClient mongoClient;

    @Inject
    MigrationService migrationService;

    @Test
    public void testUpdate() throws LiquibaseException {
        MongoDatabase database = mongoClient.getDatabase("inventory");
        Assertions.assertEquals(0, database.getCollection("Products").countDocuments());
        migrationService.update();
        Assertions.assertEquals(1, database.getCollection("Products").countDocuments());
    }
}
