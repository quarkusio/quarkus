package io.quarkus.it.mongodb.panache;

import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.it.mongodb.panache.product.ProductResource;
import io.quarkus.it.mongodb.panache.reactive.ReactiveMongodbPanacheResourceTest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@QuarkusTest
@TestProfile(ReflectionFreeSerializationTest.ReflectionFreeSerializationProfile.class)
public class ReflectionFreeSerializationTest {

    @Test
    void testObjectICustomSerialization() {
        RestAssured.get("/products")
                .then()
                .statusCode(200)
                .body("id", Matchers.equalTo(ProductResource.PRODUCT_ID));
    }

    @Test
    void testReactiveBookEntity() throws InterruptedException {
        ReactiveMongodbPanacheResourceTest.callReactiveBookEndpoint("/reactive/books/entity");
    }

    public static class ReflectionFreeSerializationProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.rest.jackson.optimization.enable-reflection-free-serializers", "true");
        }
    }
}
