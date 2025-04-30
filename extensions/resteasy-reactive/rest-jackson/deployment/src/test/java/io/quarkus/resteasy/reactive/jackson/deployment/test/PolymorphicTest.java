package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class PolymorphicTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(PolymorphicEndpoint.class, PolymorphicBase.class, PolymorphicSub.class)
                            .addAsResource(new StringAsset(""), "application.properties");
                }
            });

    @Test
    public void testSingle() {
        RestAssured.get("/poly/single")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body(Matchers.is("{\"type\":\"sub\"}"));
    }

    @Test
    public void testMany() {
        RestAssured.get("/poly/many")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body(Matchers.is("[{\"type\":\"sub\"},{\"type\":\"sub\"}]"));
    }
}
