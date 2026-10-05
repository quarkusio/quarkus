package io.quarkus.hibernate.reactive.dev;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * Reproduces https://github.com/quarkusio/quarkus/issues/56935 by restarting the application twice while the
 * recorder's static session caches survive. Without shutdown cleanup, the second restart leaves the caches
 * pointing to session factory proxies from a stopped Arc container.
 */
public class SessionFactoryRestartTest {

    @RegisterExtension
    static final QuarkusDevModeTest runner = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(Fruit.class, SessionFactoryRestartResource.class)
                    .addAsResource("application.properties"));

    @Test
    void testSessionFactoriesAfterRestart() {
        // Populate both caches with session factory proxies bound to the initial Arc container.
        assertSessions(0);

        runner.modifyResourceFile("application.properties",
                s -> s + "\nrestart.counter=1\n");
        assertSessions(1);

        runner.modifyResourceFile("application.properties",
                s -> s.replace("restart.counter=1", "restart.counter=2"));
        assertSessions(2);
    }

    private void assertSessions(int restartCounter) {
        RestAssured.when().post("/session-factory-restart/stateful")
                .then()
                .statusCode(200)
                .body("restartCounter", is(restartCounter))
                .body("fruitCount", is(1));
        RestAssured.when().post("/session-factory-restart/stateless")
                .then()
                .statusCode(200)
                .body("restartCounter", is(restartCounter))
                .body("fruitCount", is(2));
    }
}
