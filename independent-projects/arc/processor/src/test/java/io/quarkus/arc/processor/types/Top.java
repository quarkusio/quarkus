package io.quarkus.arc.processor.types;

// inheritance hierarchy:
//
// Top------>Middle------>Bottom
//    \
//     +---->Middle2
//
// Top and Bottom are in the same package,
// Middle and Middle2 are in a different package
public class Top {
    public String publicMethod(String param) {
        return null;
    }

    protected String protectedMethod(String param) {
        return null;
    }

    String packagePrivateMethod(String param) {
        return null;
    }

    private String privateMethod(String param) {
        return null;
    }

    // ---

    protected String protectedMethodToBecomePublic(String param) {
        return null;
    }

    String packagePrivateMethodToBecomeProtected(String param) {
        return null;
    }

    String packagePrivateMethodToBecomePublic(String param) {
        return null;
    }
}
