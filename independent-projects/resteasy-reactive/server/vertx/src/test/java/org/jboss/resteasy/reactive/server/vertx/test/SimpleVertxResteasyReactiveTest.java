package org.jboss.resteasy.reactive.server.vertx.test;

import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SimpleVertxResteasyReactiveTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClass(HelloResource.class);
                }
            });

    @Test
    public void helloWorldTest() {
        RestAssured.get("/hello?name=Stu")
                .then()
                .body(equalTo("hello Stu"));
    }

}
