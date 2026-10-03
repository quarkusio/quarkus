package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * When flyway-mongodb is explicitly deactivated and a bean statically injects
 * Flyway (not Instance), startup must fail with an InactiveBeanException that
 * names the config property to use for activation.
 */
@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbActiveConfigFalseStaticInjectionTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", "acfstatic")
            .overrideConfigKey("quarkus.flyway-mongodb.active", "false")
            .assertException(e -> assertThat(e)
                    .satisfies(t -> assertThat(t.getClass().getName())
                            .isEqualTo(InactiveBeanException.class.getName()))
                    .hasMessageContainingAll(
                            "Flyway MongoDB for client '<default>' was deactivated through configuration.",
                            "quarkus.flyway-mongodb.active"));

    @Inject
    MyBean myBean;

    @Test
    void startupShouldHaveFailed() {
        fail("Startup should have failed");
    }

    @ApplicationScoped
    public static class MyBean {
        @Inject
        Flyway flyway;
    }
}
