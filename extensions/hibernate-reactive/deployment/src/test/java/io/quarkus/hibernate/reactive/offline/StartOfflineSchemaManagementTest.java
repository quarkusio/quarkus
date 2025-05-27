package io.quarkus.hibernate.reactive.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.tool.schema.Action.CREATE_DROP;

import jakarta.transaction.Transactional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.entities.Hero;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test that if enable quarkus.hibernate-orm.schema-management.strategy during offline mode,
 * application start will fail
 */
public class StartOfflineSchemaManagementTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Hero.class)
                    .addAsResource("application-start-offline.properties", "application.properties"))
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", CREATE_DROP.getExternalHbm2ddlName())
            .assertException(
                    throwable -> assertThat(throwable)
                            .hasNoSuppressedExceptions()
                            .hasMessageContaining(
                                    "When using offline mode with `quarkus.hibernate-orm.database.start-offline=true`, the schema management strategy `quarkus.hibernate-orm.schema-management.strategy` must be unset or set to `none`"));

    @Test
    @Transactional
    public void applicationStarts() {
        Assertions.fail("Startup has failed");
    }

}
