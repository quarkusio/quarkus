package io.quarkus.smallrye.health.test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

public class HealthUnitTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BasicHealthCheck.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testHealth() {
        // the health check does not set a content type so we need to force the parser
        try {
            RestAssured.defaultParser = Parser.JSON;
            RestAssured.when().get("/health").then()
                    .body("outcome", is("UP"),
                            "checks.state", contains("UP"),
                            "checks.name", contains("basic"));
        } finally {
            RestAssured.reset();
        }
    }

}
