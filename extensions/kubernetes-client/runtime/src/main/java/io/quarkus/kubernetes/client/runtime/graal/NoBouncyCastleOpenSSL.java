package io.quarkus.kubernetes.client.runtime.graal;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

class NoBouncyCastleOpenSSL implements BooleanSupplier {
    static final String ORG_BOUNCYCASTLE_OPENSSL_PACKAGE = "org.bouncycastle.openssl";
    static final Boolean ORG_BOUNCYCASTLE_OPENSSL_AVAILABLE = Arrays.asList(Package.getPackages()).stream()
            .map(p -> p.getName()).anyMatch(p -> p.startsWith(ORG_BOUNCYCASTLE_OPENSSL_PACKAGE));

    @Override
    public boolean getAsBoolean() {
        return !ORG_BOUNCYCASTLE_OPENSSL_AVAILABLE;
    }
}