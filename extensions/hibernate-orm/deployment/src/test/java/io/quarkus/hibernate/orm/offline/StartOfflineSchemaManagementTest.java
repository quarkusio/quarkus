package io.quarkus.hibernate.orm.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.tool.schema.Action.CREATE_DROP;

import jakarta.transaction.Transactional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test that if enable quarkus.hibernate-orm.schema-management.strategy during offline mode,
 * application start will fail
 */
public class StartOfflineSchemaManagementTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsResource("application-start-offline.properties", "application.properties"))
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", CREATE_DROP.getExternalHbm2ddlName())
            .assertException(
                    throwable -> assertThat(throwable)
                            .hasNoSuppressedExceptions()
                            .hasMessageContaining(
                                    "Version check `quarkus.hibernate-orm.schema-management.strategy` cannot be any different than `none` when using offline mode `quarkus.hibernate-orm.database.start-offline=true`"));

    @Test
    @Transactional
    public void applicationStarts() {
        Assertions.fail("Startup has failed");
    }

}
