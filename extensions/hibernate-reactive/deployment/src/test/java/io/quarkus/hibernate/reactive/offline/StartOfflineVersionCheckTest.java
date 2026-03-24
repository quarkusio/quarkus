package io.quarkus.hibernate.reactive.offline;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.transaction.Transactional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.entities.Hero;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Test that if enable versionCheckEnabled during offline mode,
 * application start will fail
 */
public class StartOfflineVersionCheckTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Hero.class)
                    .addAsResource("application-start-offline.properties", "application.properties"))
            .overrideConfigKey("quarkus.hibernate-orm.database.version-check.enabled", "true")
            .assertException(
                    throwable -> assertThat(throwable)
                            .hasNoSuppressedExceptions()
                            .hasMessageContaining(
                                    "When using offline mode `quarkus.hibernate-orm.database.start-offline=true`, version check `quarkus.hibernate-orm.database.version-check.enabled` must be unset or set to `false`"));

    @Test
    @Transactional
    public void applicationStarts() {
        Assertions.fail("Startup has failed");
    }

}
