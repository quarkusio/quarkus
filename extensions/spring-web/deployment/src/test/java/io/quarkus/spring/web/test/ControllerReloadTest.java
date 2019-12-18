package io.quarkus.spring.web.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class ControllerReloadTest {

    @RegisterExtension
    static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SimpleSpringController.class));

    @Test
    public void testRepositoryIsReloaded() {
        when().get("/simple").then().body(is("hello"));

        TEST.modifySourceFile("SimpleSpringController.java", s -> s.replace("hello", "hi"));

        when().get("/simple").then().body(is("hi"));
    }
}
