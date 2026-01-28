package io.quarkus.it.aesh;

import io.quarkus.test.QuarkusProdModeTest;

class AeshTestUtils {

    private AeshTestUtils() {
    }

    static QuarkusProdModeTest createConfig(String appName, Class<?>... classes) {
        return new QuarkusProdModeTest()
                .withApplicationRoot((jar) -> jar.addClasses(classes))
                .setApplicationName(appName).setApplicationVersion("0.1-SNAPSHOT")
                .setExpectExit(true).setRun(true);
    }
}
