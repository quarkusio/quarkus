package io.quarkus.test.junit.callback;

import org.junit.jupiter.api.extension.ExtensionContext;

public interface QuarkusTestAfterEachCallback {

    void afterEach(ExtensionContext context, boolean isSubstrateTest);
}
