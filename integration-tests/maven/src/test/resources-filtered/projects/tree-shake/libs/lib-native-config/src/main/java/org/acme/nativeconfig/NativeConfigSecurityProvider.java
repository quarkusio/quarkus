package org.acme.nativeconfig;

public class NativeConfigSecurityProvider extends java.security.Provider {

    public NativeConfigSecurityProvider() {
        super("NativeConfigSecurityProvider", "1.0", "tree-shake test provider");
    }
}
