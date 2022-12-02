package io.quarkus.it.smallrye.config;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;
import java.util.logging.LogManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.InMemoryLogHandler;
import io.quarkus.test.junit.QuarkusTestExtension;

public class HibernatePropertiesTest {
    private static final java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("io.quarkus.config");
    private static final InMemoryLogHandler inMemoryLogHandler = new InMemoryLogHandler(
            record -> record.getLevel().intValue() >= Level.WARNING.intValue());

    static {
        rootLogger.addHandler(inMemoryLogHandler);
    }

    // So we can use .env. We don't have support in the other extensions to pass in external files to the application jar.
    @RegisterExtension
    static QuarkusTestExtension TEST = new QuarkusTestExtension() {
        @Override
        public void beforeAll(final ExtensionContext context) throws Exception {
            super.beforeAll(context);
            assertTrue(inMemoryLogHandler.getRecords().isEmpty());
        }
    };

    @Test
    void properties() {
        given()
                .get("/users")
                .then()
                .statusCode(OK.getStatusCode());
    }
}
