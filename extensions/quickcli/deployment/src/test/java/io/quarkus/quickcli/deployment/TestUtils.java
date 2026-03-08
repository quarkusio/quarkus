package io.quarkus.quickcli.deployment;

import io.quarkus.test.QuarkusProdModeTest;

class TestUtils {

    private TestUtils() {
    }

    static QuarkusProdModeTest createConfig(String appName, Class<?>... classes) {
        return new QuarkusProdModeTest()
                .withApplicationRoot((jar) -> {
                    jar.addClasses(classes);
                    // Also add the generated QuickCLI model classes
                    for (Class<?> cls : classes) {
                        String modelName = cls.getName() + "_QuickCliModel";
                        try {
                            Class<?> modelClass = Class.forName(modelName);
                            jar.addClass(modelClass);
                        } catch (ClassNotFoundException e) {
                            // Not a @Command class — no model generated, that's fine
                        }
                    }
                })
                .setApplicationName(appName).setApplicationVersion("0.1-SNAPSHOT")
                .setExpectExit(true).setRun(true);
    }
}
