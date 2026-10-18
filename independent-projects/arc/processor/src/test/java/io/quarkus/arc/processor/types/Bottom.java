package io.quarkus.arc.processor.types;

import io.quarkus.arc.processor.types.extrapkg.Middle;

public class Bottom extends Middle {
    @Override
    public String publicMethod(String param) {
        return null;
    }

    @Override
    protected String protectedMethod(String param) {
        return null;
    }

    @Override
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

    @Override
    protected String packagePrivateMethodToBecomeProtected(String param) {
        return null;
    }

    @Override
    public String packagePrivateMethodToBecomePublic(String param) {
        return null;
    }
}
