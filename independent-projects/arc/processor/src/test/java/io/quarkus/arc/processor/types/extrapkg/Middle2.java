package io.quarkus.arc.processor.types.extrapkg;

import io.quarkus.arc.processor.types.Top;

public class Middle2 extends Top {
    @Override
    public String publicMethod(String param) {
        return null;
    }

    @Override
    protected String protectedMethod(String param) {
        return null;
    }

    // no override
    String packagePrivateMethod(String param) {
        return null;
    }

    // no override
    private String privateMethod(String param) {
        return null;
    }

    // ---

    @Override
    public String protectedMethodToBecomePublic(String param) {
        return null;
    }

    // no override
    protected String packagePrivateMethodToBecomeProtected(String param) {
        return null;
    }

    // no override
    public String packagePrivateMethodToBecomePublic(String param) {
        return null;
    }
}
