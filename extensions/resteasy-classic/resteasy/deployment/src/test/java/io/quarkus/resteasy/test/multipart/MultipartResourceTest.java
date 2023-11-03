package io.quarkus.resteasy.test.multipart;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.ContentType;

public class MultipartResourceTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MultipartResource.class));

    @Test
    public void testHelloEndpoint() {
        Map<String, String> map = new HashMap<>();
        map.put("test", "value");

        given()
                .formParams(map)
                .header("Expect", "100-continue")
                .contentType(ContentType.URLENC)
                .when().post("/multipart/")
                .then()
                .statusCode(200)
                .body(is("[test:value]"));
    }

}
