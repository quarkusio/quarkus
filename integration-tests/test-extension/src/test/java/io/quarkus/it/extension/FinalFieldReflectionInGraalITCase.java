package io.quarkus.it.extension;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.SubstrateTest;
import io.restassured.RestAssured;

// TODO replace with @NativeImageTest once @SubstrateTest is removed.
// Use @SubstrateTest for now to ensure backward compatibility.
@SubstrateTest
public class FinalFieldReflectionInGraalITCase {

    @Test
    public void testFieldAndGetterReflectionOnEntityFromServlet() {
        RestAssured.when().get("/core/reflection/final").then()
                .body(is("OK"));
    }

}
