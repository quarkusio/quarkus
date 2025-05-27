package io.quarkus.hibernate.orm.offline;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.transaction.Transactional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test that if enable versionCheckEnabled during offline mode,
 * application start will fail
 */
public class StartOfflineVersionCheckTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
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
