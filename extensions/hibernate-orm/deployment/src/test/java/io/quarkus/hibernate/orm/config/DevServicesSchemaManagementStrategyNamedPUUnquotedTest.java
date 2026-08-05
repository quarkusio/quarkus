package io.quarkus.hibernate.orm.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.config.namedpu.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that Dev Services automatically sets schema-management.strategy=drop-and-create
 * for a named persistence unit configured with unquoted keys.
 */
public class DevServicesSchemaManagementStrategyNamedPUUnquotedTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(MyEntity.class.getPackage().getName()))
            .overrideConfigKey("quarkus.hibernate-orm.pu-1.datasource", "<default>")
            .overrideConfigKey("quarkus.hibernate-orm.pu-1.packages", MyEntity.class.getPackageName());

    @Test
    public void testDevServicesSchemaManagementStrategy() {
        assertThat(ConfigProvider.getConfig()
                .getValue("quarkus.hibernate-orm.pu-1.schema-management.strategy", String.class))
                .isEqualTo("drop-and-create");
        assertThat(ConfigProvider.getConfig()
                .getValue("quarkus.hibernate-orm.\"pu-1\".schema-management.strategy", String.class))
                .isEqualTo("drop-and-create");
    }

}
