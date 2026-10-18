package io.quarkus.test.reload;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

// https://github.com/quarkusio/quarkus/issues/35381
public class CrashAfterReloadTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setAllowFailedStart(true)
            .withApplicationRoot(root -> root.addClasses(SomeBean.class, SomeBeanClient.class));

    @Test
    public void testRestarts() {
        // The build should fail initially
        RestAssured.with()
                .get("/test")
                .then()
                .statusCode(500);

        String someBeanWithoutScope = "public class SomeBean {";
        String someBeanWithScope = "@ApplicationScoped public class SomeBean {";
        String originalImport = "import jakarta.annotation.PostConstruct;";
        String newImport = "import jakarta.annotation.PostConstruct;\nimport jakarta.enterprise.context.ApplicationScoped;";

        // Add the scope annotation
        config.modifySourceFile(SomeBean.class,
                s -> s.replace(someBeanWithoutScope, someBeanWithScope).replace(originalImport, newImport));

        RestAssured.with()
                .get("/test")
                .then()
                .statusCode(200)
                .body(containsString("pong"));

        // Remove the scope annotation - the build should fail but the dev mode should not exit
        config.modifySourceFile(SomeBean.class,
                s -> s.replace(someBeanWithScope, someBeanWithoutScope));

        RestAssured.with()
                .get("/test")
                .then()
                .statusCode(500);

        // Add the scope annotation
        config.modifySourceFile(SomeBean.class,
                s -> s.replace(someBeanWithoutScope, someBeanWithScope));

        RestAssured.with()
                .get("/test")
                .then()
                .statusCode(200)
                .body(containsString("pong"));

    }

}
