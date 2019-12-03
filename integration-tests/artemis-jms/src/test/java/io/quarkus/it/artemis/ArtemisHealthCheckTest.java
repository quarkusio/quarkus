package io.quarkus.it.artemis;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.artemis.test.ArtemisTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;

@QuarkusTest
@QuarkusTestResource(ArtemisTestResource.class)
public class ArtemisHealthCheckTest {

    @Test
    public void test() {
        Response response = RestAssured.with().get("/health/ready");
        Assertions.assertEquals(Status.OK.getStatusCode(), response.statusCode());

        Map<String, Object> body = response.as(new TypeRef<Map<String, Object>>() {
        });
        Assertions.assertEquals("UP", body.get("status"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) body.get("checks");
        Assertions.assertEquals(1, checks.size());
        Assertions.assertEquals("Artemis JMS health check", checks.get(0).get("name"));
    }
}
