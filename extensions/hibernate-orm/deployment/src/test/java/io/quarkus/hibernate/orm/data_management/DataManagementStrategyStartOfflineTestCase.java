package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Explicitly requesting the data init script to be executed while starting offline is an error,
 * since it would require connecting to the database on startup.
 */
public class DataManagementStrategyStartOfflineTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsResource("data.sql")
                    .addAsResource("application-start-offline.properties", "application.properties"))
            .overrideRuntimeConfigKey("quarkus.hibernate-orm.data-management.strategy", "create")
            .assertException(
                    throwable -> assertThat(throwable)
                            .hasMessageContaining(
                                    "When using offline mode with `quarkus.hibernate-orm.database.start-offline=true`, the data management strategy `quarkus.hibernate-orm.data-management.strategy` must be unset or set to `none`"));

    @Test
    public void applicationStarts() {
        Assertions.fail("Startup has failed");
    }
}
