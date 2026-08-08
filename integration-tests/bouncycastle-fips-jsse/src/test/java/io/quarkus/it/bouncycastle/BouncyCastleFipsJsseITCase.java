package io.quarkus.it.bouncycastle;

import static io.quarkus.test.junit.QuarkusIntegrationTestExtension.TEST_LOG_PATH;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.value.registry.ValueRegistry;

@QuarkusIntegrationTest
public class BouncyCastleFipsJsseITCase extends BouncyCastleFipsJsseTestCase {

    // Injected automatically by QuarkusIntegrationTestExtension
    private ValueRegistry valueRegistry;

    @Override
    protected Path getLogPath() {
        if (valueRegistry != null && valueRegistry.containsKey(TEST_LOG_PATH)) {
            return valueRegistry.get(TEST_LOG_PATH);
        }
        return super.getLogPath();
    }

    @Test
    @DisabledOnIntegrationTest
    @Override
    public void testListProviders() throws Exception {
        doTestListProviders();
        checkLog(true);
    }
}
