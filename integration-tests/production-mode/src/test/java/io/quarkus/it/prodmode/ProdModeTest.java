package io.quarkus.it.prodmode;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class ProdModeTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ProductionModeTestsEndpoint.class, ForkJoinPoolAssertions.class))
            .setApplicationName("prod-mode-quarkus")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setRun(true);

    @Test
    public void basicApplicationAliveTest() {
        given()
                .when().get("/production-mode-tests")
                .then()
                .statusCode(200)
                .body(is("hello"));
    }

    @Test
    public void areExpectedSystemPropertiesSet() {
        given()
                .when().get("/production-mode-tests/areExpectedSystemPropertiesSet")
                .then()
                .statusCode(200)
                .body(is("yes"));
    }

    @Test
    public void isForkJoinPoolUsingExpectedClassloader() {
        given()
                .when().get("/production-mode-tests/isForkJoinPoolUsingExpectedClassloader")
                .then()
                .statusCode(200)
                .body(is("yes"));
    }

}
