package io.quarkus.it.bouncycastle;

import java.nio.file.Path;

import io.quarkus.test.common.TestLog;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class BouncyCastleJsseITCase extends BouncyCastleJsseTestCase {

    // Injected automatically by QuarkusIntegrationTestExtension
    private TestLog testLog;

    @Override
    protected Path getLogPath() {
        return testLog.getLogFilePath();
    }

    @Test
    @Override
    public void testListProviders() {
        doTestListProviders();
        checkLog(true);
    }
}
