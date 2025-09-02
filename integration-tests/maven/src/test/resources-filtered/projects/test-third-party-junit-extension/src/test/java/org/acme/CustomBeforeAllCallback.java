package org.acme;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

public class CustomBeforeAllCallback implements BeforeAllCallback {
    @Override
    public void beforeAll(ExtensionContext context) {
        // Under the covers, this uses a service loader
        try {
            DockerClientFactory.instance()
                    .dockerHostIpAddress();
        } catch (IllegalStateException e) {
            // Docker won't work on windows CI
            if (System.getProperty("os.name").contains("indows") && e.getMessage().contains("valid Docker environment")) {
                // This is expected on windows
            } else {
                throw e;
            }
        }
    }
}
