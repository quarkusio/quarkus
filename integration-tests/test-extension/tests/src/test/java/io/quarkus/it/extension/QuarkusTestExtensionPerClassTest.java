package io.quarkus.it.extension;

import static io.quarkus.it.extension.ConfigManager.TEST_PROPERTY_NAME;
import static io.quarkus.it.extension.ConfigManager.TEST_PROPERTY_VALUE;
import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTestResource(ConfigManager.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QuarkusTestExtensionPerClassTest {

    @Test
    @Order(1)
    void testFirst() {
        assertThat(getPropertyValue())
                .isEqualTo(TEST_PROPERTY_VALUE);
    }

    @Test
    @Order(2)
    void testSecond() {
        assertThat(getPropertyValue())
                .isEqualTo(TEST_PROPERTY_VALUE);
    }

    private String getPropertyValue() {
        return ConfigProvider.getConfig()
                .getConfigValue(TEST_PROPERTY_NAME)
                .getValue();
    }
}
