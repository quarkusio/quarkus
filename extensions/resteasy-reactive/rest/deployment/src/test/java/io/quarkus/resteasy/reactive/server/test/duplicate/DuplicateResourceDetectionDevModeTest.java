package io.quarkus.resteasy.reactive.server.test.duplicate;

import static io.restassured.RestAssured.when;

import java.util.function.Function;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class DuplicateResourceDetectionDevModeTest {

    @RegisterExtension
    static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(GreetingResource3.class, GreetingResource4.class))
            .setAllowFailedStart(true);

    @Test
    public void testRestarts() {
        // The build should fail initially
        when()
                .get("/hello-resteasy")
                .then()
                .statusCode(500);

        TEST.modifySourceFile(GreetingResource4.class.getSimpleName() + ".java", new Function<>() {
            @Override
            public String apply(String s) {
                return s.replace("hello-resteasy", "hello-resteasy2");
            }
        });

        // after changing the paths to remove the collision, the endpoints should work
        when()
                .get("/hello-resteasy")
                .then()
                .statusCode(200);
        when()
                .get("/hello-resteasy2")
                .then()
                .statusCode(200);

    }
}
