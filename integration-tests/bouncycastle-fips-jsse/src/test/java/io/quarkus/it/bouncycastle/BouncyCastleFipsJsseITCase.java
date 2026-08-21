package io.quarkus.it.bouncycastle;

import java.nio.file.Path;

import io.quarkus.test.common.TestLog;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class BouncyCastleFipsJsseITCase extends BouncyCastleFipsJsseTestCase {

    // Injected automatically by QuarkusIntegrationTestExtension
    private TestLog testLog;

    @Override
    protected Path getLogPath() {
        return testLog.getLogFilePath();
    }

    @Test
    @DisabledOnIntegrationTest
    @Override
    public void testListProviders() throws Exception {
        doTestListProviders();
        checkLog(true);
    }
}
