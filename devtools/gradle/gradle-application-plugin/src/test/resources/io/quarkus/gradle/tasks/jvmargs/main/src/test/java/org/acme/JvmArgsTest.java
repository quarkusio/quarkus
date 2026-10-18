package org.acme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Verifies that the Quarkus Gradle plugin properly configures JVM args and]
 * system property (logging manager) for test execution.
 */
public class JvmArgsTest {

    private static final String LOGGING_MANAGER_PROP_NAME = "java.util.logging.manager";
    private static final String LOGGING_MANAGER_PROP_VALUE = "org.jboss.logmanager.LogManager";
    private static final String OPENS_LANG_INVOKE = "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED";
    private static final String OPENS_LANG = "--add-opens=java.base/java.lang=ALL-UNNAMED";
    private static final String EXPORTS_INTERNAL_MODULE = "--add-exports=java.base/jdk.internal.module=ALL-UNNAMED";

    @Test
    public void testJvmArgsAndSystemProperties() throws IOException {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> jvmArgs = runtimeMxBean.getInputArguments();

        File outputFile = new File("build/test-results/jvm-config.txt");
        outputFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write("=== JVM Arguments ===\n");
            for (String arg : jvmArgs) {
                writer.write(arg + "\n");
            }

            writer.write("\n=== System Properties ===\n");
            String loggingManager = System.getProperty(LOGGING_MANAGER_PROP_NAME);
            writer.write("%s=".formatted(LOGGING_MANAGER_PROP_NAME) + loggingManager + "\n");
        }

        assertEquals(LOGGING_MANAGER_PROP_VALUE,
                System.getProperty(LOGGING_MANAGER_PROP_NAME),
                "%s should be set to %s".formatted(LOGGING_MANAGER_PROP_NAME, LOGGING_MANAGER_PROP_VALUE));

        assertTrue(jvmArgs.stream().anyMatch(arg -> arg.contains(OPENS_LANG_INVOKE)),
                "JVM args should contain %s".formatted(OPENS_LANG_INVOKE));

        assertTrue(jvmArgs.stream().anyMatch(arg -> arg.contains(OPENS_LANG)),
                "JVM args should contain %s".formatted(OPENS_LANG));

        assertTrue(jvmArgs.stream().anyMatch(arg -> arg.contains(EXPORTS_INTERNAL_MODULE)),
                "JVM args should contain %s".formatted(EXPORTS_INTERNAL_MODULE));
    }
}
