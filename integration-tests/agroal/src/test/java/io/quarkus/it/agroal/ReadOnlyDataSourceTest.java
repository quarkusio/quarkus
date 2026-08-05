package io.quarkus.it.agroal;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Our Windows CI does not have Docker installed properly")
public class ReadOnlyDataSourceTest {

    @Test
    public void defaultPoolIsNotReadOnly() {
        when().get("/agroal/read-only-test/default")
                .then()
                .statusCode(200)
                // agroalReadOnly,connectionReadOnly
                .body(is("false,false"));
    }

    @Test
    public void namedPoolIsReadOnly() {
        when().get("/agroal/read-only-test/readonly")
                .then()
                .statusCode(200)
                // agroalReadOnly,connectionReadOnly
                .body(is("true,true"));
    }
}
