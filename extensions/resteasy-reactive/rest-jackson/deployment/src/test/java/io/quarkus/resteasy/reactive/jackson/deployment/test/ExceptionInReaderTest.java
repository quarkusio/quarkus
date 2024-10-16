package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ExceptionInReaderTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(FroMage.class, FroMageEndpoint.class);
                }
            });

    @Test
    public void test() {
        RestAssured.with().contentType("application/json").body("{\"price\": \"ten\"}").put("/fromage")
                .then().statusCode(400);
    }
}
