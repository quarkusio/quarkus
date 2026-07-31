package io.quarkus.vertx.http.tls;

import jakarta.inject.Inject;

import io.netty.handler.ssl.OpenSsl;
import io.vertx.core.Vertx;

public abstract class AbstractHybridKeyExchangeTest {

    // Evaluated once at class-load time, before any QuarkusClassLoader is created.
    // Repeated calls during @EnabledIf are unsafe: after a Quarkus instance shuts down
    // its classloader may be GC'd, triggering JNI_OnUnload which deregisters SSL native
    // methods globally while UNAVAILABILITY_CAUSE stays null (isAvailable() still true).
    private static final boolean OPENSSL_35_AVAILABLE = checkOpenSsl35();

    private static boolean checkOpenSsl35() {
        return OpenSsl.isAvailable() && OpenSsl.version() >= 0x30500000L;
    }

    @Inject
    Vertx vertx;

    static boolean isOpenSsl35Available() {
        return OPENSSL_35_AVAILABLE;
    }

    static boolean isJdk27OrLater() {
        return Runtime.version().feature() >= 27;
    }

}
