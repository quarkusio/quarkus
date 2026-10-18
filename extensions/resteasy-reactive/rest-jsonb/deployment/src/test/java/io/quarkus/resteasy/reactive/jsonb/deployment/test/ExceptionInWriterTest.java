package io.quarkus.resteasy.reactive.jsonb.deployment.test;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class ExceptionInWriterTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Cheese.class, CheeseEndpoint.class);
                }
            });

    @Test
    public void test() {
        RestAssured.with().header("Accept", "text/plain", "application/json").get("/cheese")
                .then().statusCode(500);
    }
}
