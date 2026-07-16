package io.quarkus.vertx.http.tls;

import jakarta.inject.Inject;

import io.netty.handler.ssl.OpenSsl;
import io.vertx.core.Vertx;

public abstract class AbstractHybridKeyExchangeTest {

    @Inject
    Vertx vertx;

    static boolean isOpenSsl35Available() {
        return OpenSsl.isAvailable() && OpenSsl.version() >= 0x30500000L;
    }

}
