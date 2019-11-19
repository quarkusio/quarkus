package io.quarkus.it.extension;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.NativeImageTest;
import io.restassured.RestAssured;

@NativeImageTest
public class FinalFieldReflectionInGraalITCase {

    @Test
    public void testFieldAndGetterReflectionOnEntityFromServlet() {
        RestAssured.when().get("/core/reflection/final").then()
                .body(is("OK"));
    }

}
