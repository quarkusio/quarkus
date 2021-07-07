package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.NativeImageTest;
import io.restassured.RestAssured;

@NativeImageTest
public class CoreSerializationInGraalITCase {

    @Test
    public void testEntitySerializationFromServlet() throws Exception {
        RestAssured.when().get("/core/serialization").then()
                .body(is("OK"));
    }

}
