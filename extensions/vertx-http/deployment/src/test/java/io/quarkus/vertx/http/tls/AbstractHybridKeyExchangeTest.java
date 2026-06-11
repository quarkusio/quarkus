package io.quarkus.vertx.http.tls;

import jakarta.inject.Inject;

import org.junit.jupiter.api.condition.EnabledIf;

import io.netty.handler.ssl.OpenSsl;
import io.vertx.core.Vertx;

@EnabledIf("isOpenSsl35Available")
public abstract class AbstractHybridKeyExchangeTest {

    @Inject
    Vertx vertx;

    static boolean isOpenSsl35Available() {
        return OpenSsl.isAvailable() && OpenSsl.version() >= 0x30500000L;
    }

}
