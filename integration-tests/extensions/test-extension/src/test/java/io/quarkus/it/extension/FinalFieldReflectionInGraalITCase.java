package io.quarkus.it.extension;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.SubstrateTest;
import io.restassured.RestAssured;

@SubstrateTest
public class FinalFieldReflectionInGraalITCase {

    @Test
    public void testFieldAndGetterReflectionOnEntityFromServlet() throws Exception {
        RestAssured.when().get("/core/reflection/final").then()
                .body(is("OK"));
    }

}
