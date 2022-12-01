package io.quarkus.gradle;

import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.quarkus.devtools.testing.RegistryClientTestHelper;

public class QuarkusGradleDevToolsTestBase extends QuarkusGradleWrapperTestBase {

    private static Properties devToolsProps = new Properties();

    @BeforeAll
    static void enableDevToolsTestConfig() {
        RegistryClientTestHelper.enableRegistryClientTestConfig(Paths.get("").normalize().toAbsolutePath().resolve("build"),
                devToolsProps);
        for (Map.Entry<?, ?> prop : devToolsProps.entrySet()) {
            System.setProperty(prop.getKey().toString(), prop.getValue().toString());
        }
    }

    @AfterAll
    static void disableDevToolsTestConfig() {
        RegistryClientTestHelper.disableRegistryClientTestConfig();
    }

    @Override
    protected void setupTestCommand() {
        for (Map.Entry<?, ?> prop : devToolsProps.entrySet()) {
            setSystemProperty(prop.getKey().toString(), prop.getValue().toString());
        }
    }
}
