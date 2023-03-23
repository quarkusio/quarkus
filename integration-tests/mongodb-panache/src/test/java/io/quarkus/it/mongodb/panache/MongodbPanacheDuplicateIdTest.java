package io.quarkus.it.mongodb.panache;

import static io.restassured.RestAssured.get;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoReplicaSetTestResource;

@QuarkusTest
@QuarkusTestResource(MongoReplicaSetTestResource.class)
//@DisabledOnOs(OS.WINDOWS)
class MongodbPanacheDuplicateIdTest {

    @Test
    public void testMoreEntityFunctionalities() {
        get("/testDuplicateId/imperative/entity").then().statusCode(200);
    }

    @Test
    public void testMoreRepositoryFunctionalities() {
        get("/testDuplicateId/imperative/repository").then().statusCode(200);
    }
}
