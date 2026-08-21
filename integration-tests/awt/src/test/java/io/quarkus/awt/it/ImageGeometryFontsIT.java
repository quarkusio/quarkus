package io.quarkus.awt.it;

import java.nio.file.Path;

import io.quarkus.test.common.TestLog;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class ImageGeometryFontsIT extends ImageGeometryFontsTest {

    // Injected automatically by QuarkusIntegrationTestExtension
    private TestLog testLog;

    @Override
    protected Path getLogPath() {
        return testLog.getLogFilePath();
    }
}
