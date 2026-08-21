package io.quarkus.it.keycloak;

import java.nio.file.Path;

import io.quarkus.test.common.TestLog;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class CodeFlowAuthorizationInGraalITCase extends CodeFlowAuthorizationTest {

    // Injected automatically by QuarkusIntegrationTestExtension
    private TestLog testLog;

    @Override
    protected Path getLogPath() {
        return testLog.getLogFilePath();
    }
}
