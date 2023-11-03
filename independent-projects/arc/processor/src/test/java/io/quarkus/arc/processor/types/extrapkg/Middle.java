package io.quarkus.arc.processor.types.extrapkg;

import io.quarkus.arc.processor.types.Top;

public class Middle extends Top {
    // no override
    String packagePrivateMethod(String param) {
        return null;
    }

    // no override
    private String privateMethod(String param) {
        return null;
    }

    // ---

    // no override
    String packagePrivateMethodToBecomeProtected(String param) {
        return null;
    }

    // no override
    String packagePrivateMethodToBecomePublic(String param) {
        return null;
    }
}
