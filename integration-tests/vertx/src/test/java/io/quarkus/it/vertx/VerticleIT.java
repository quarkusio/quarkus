package io.quarkus.it.vertx;

import static io.quarkus.test.junit.QuarkusIntegrationTestExtension.TEST_LOG_PATH;

import java.nio.file.Path;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.value.registry.ValueRegistry;

@QuarkusIntegrationTest
public class VerticleIT extends VerticleTest {

    // Injected automatically by QuarkusIntegrationTestExtension
    private ValueRegistry valueRegistry;

    @Override
    protected Path getLogPath() {
        if (valueRegistry != null && valueRegistry.containsKey(TEST_LOG_PATH)) {
            return valueRegistry.get(TEST_LOG_PATH);
        }
        return super.getLogPath();
    }
}
