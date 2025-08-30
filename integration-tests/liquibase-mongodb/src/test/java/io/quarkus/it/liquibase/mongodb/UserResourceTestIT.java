package io.quarkus.it.liquibase.mongodb;

import static io.restassured.RestAssured.get;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.restassured.common.mapper.TypeRef;

@QuarkusIntegrationTest
@QuarkusTestResource(value = MongoTestResource.class, initArgs = @ResourceArg(name = "port", value = "27019"))
@DisabledOnOs(OS.WINDOWS)
class UserResourceTestIT {
    @Test
    public void testTheEndpoint() {
        // assert that a fruit exist as one has been created in the changelog
        List<User> list = get("/users").as(new TypeRef<>() {
        });
        Assertions.assertEquals(1, list.size());
    }
}
