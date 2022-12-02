package io.quarkus.it.liquibase.mongodb;

import static io.restassured.RestAssured.get;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.restassured.common.mapper.TypeRef;

@QuarkusIntegrationTest
@QuarkusTestResource(MongoTestResource.class)
@DisabledOnOs(OS.WINDOWS)
class NativeFruitResourceTestIT {
    @Test
    public void testTheEndpoint() {
        // assert that a fruit exist as one has been created in the changelog
        List<Fruit> list = get("/fruits").as(new TypeRef<List<Fruit>>() {
        });
        Assertions.assertEquals(1, list.size());
    }
}
