package io.quarkus.test.junit.callback;

import org.junit.jupiter.api.extension.ExtensionContext;

public interface QuarkusTestBeforeEachCallback {

    void beforeEach(ExtensionContext context, boolean isSubstrateTest);
}
