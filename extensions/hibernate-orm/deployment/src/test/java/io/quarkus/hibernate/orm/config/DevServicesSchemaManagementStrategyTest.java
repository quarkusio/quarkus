package io.quarkus.hibernate.orm.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class DevServicesSchemaManagementStrategyTest {

    // A simple runner like this will trigger Dev Services
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withEmptyApplication();

    @Test
    public void testDevServices() {
        String value = ConfigProvider.getConfig()
                .getValue("quarkus.hibernate-orm.schema-management.strategy", String.class);
        assertThat(value).isEqualTo("drop-and-create");
    }

}
