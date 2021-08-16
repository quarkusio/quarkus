package org.jboss.resteasy.reactive.server.vertx.test.multipart;

import io.restassured.RestAssured;
import java.util.function.Supplier;
import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RawMultipartTestCase {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClass(EchoResource.class);
                }
            });

    @Test
    public void testHelloEndpoint() {
        RestAssured.given()
                .when()
                .multiPart("testName", "testValue")
                .post("/echo")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("testValue"));
    }

}
