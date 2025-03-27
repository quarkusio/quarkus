package io.quarkus.spring.web.test;

import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class MapResourceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MapResourceTest.MapControllers.class));

    @Test
    void ok() {
        RestAssured.when().get("/another/ok?framework=quarkus")
                .then()
                .statusCode(200)
                .body(Matchers.is("quarkus"));
    }

    @Path("/quarkus")
    public static class MapControllers {
        @GET
        public String ok(@RestQuery Map<String, String> queryParams) {
            return queryParams.get("framework");
        }
    }
}
