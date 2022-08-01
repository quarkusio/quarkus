package io.quarkus.devtools.testing;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class RegistryClientTest implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        RegistryClientTestHelper.enableRegistryClientTestConfig();
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        RegistryClientTestHelper.disableRegistryClientTestConfig();
    }
}
