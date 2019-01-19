package org.jboss.shamrock.health.test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import org.jboss.shamrock.test.ShamrockUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;


public class HealthUnitTest {

    @RegisterExtension
    static final ShamrockUnitTest config = new ShamrockUnitTest()
            .setArchiveProducer(() ->
                    ShrinkWrap.create(JavaArchive.class)
                            .addClasses(BasicHealthCheck.class)
                            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
            );

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
