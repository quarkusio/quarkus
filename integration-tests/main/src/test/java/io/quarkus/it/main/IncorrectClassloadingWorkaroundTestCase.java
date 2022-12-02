package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

//https://github.com/quarkusio/quarkus/issues/17175
@QuarkusTest
public class IncorrectClassloadingWorkaroundTestCase {

    /**
     * libraries should load from the Thread Context Class Loader
     * we test that even if libraries do the wrong thing our workaround still works
     * without the need to force flat Class-Path
     */
    @Test
    public void testClassloadingStillWorksWhenLibrariesLoadFromWrongCL() {
        RestAssured.when().get("/shared/classloading").then()
                .body(is("Wrong Classloading"));
    }

}
