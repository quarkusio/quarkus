package io.quarkus.undertow.test;

import static org.hamcrest.Matchers.is;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ServletDestroyTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PreDestroyServlet.class))
            .setAllowTestClassOutsideDeployment(true)
            .setAfterUndeployListener(() -> {
                try {
                    Assertions.assertEquals("Servlet Destroyed", Messages.MESSAGES.poll(2, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

    @Test
    public void testServlet() {
        RestAssured.when().get("/destroy").then()
                .statusCode(200)
                .body(is("pre destroy servlet"));
    }

}
