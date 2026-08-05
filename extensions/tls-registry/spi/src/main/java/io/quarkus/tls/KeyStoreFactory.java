package io.quarkus.tls;

import io.vertx.core.Vertx;

/**
 * A factory for creating {@link KeyStoreAndKeyCertOptions} from an {@code other} keystore configuration.
 * <p>
 * Implementations must be CDI beans annotated with {@code @Identifier("<type>")} where {@code <type>}
 * matches the {@code quarkus.tls.key-store.other.type} value.
 * <p>
 * This method is never called on a Vert.x event loop thread. It is called on the main thread during
 * application startup and on a worker thread during certificate reload. Blocking operations (file I/O,
 * network calls, HSM access) are safe to perform.
 */
@FunctionalInterface
public interface KeyStoreFactory {

    /**
     * Creates the keystore and Vert.x options from the given configuration.
     * <p>
     * This method is called on the main thread at startup and on a Vert.x worker thread during
     * certificate reload. Blocking operations are safe.
     *
     * @param config the "other" keystore configuration
     * @param vertx the managed Vert.x instance
     * @param name the TLS bucket name ({@code <default>} for the default configuration)
     * @return the keystore and options, must not be {@code null}
     */
    KeyStoreAndKeyCertOptions createKeyStore(OtherKeyStoreConfiguration config, Vertx vertx, String name);
}
