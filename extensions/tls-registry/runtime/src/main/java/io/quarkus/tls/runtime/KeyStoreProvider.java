package io.quarkus.tls.runtime;

import io.vertx.core.Vertx;

/**
 * An interface for providing {@link KeyStoreAndKeyCertOptions} from CDI beans at runtime.
 */
@FunctionalInterface
public interface KeyStoreProvider {

    /**
     * Returns the keystore and options to be used for [re]loading the state of a TLS configuration
     *
     * @param vertx the managed vertx instance
     * @return the keystore and options
     */
    KeyStoreAndKeyCertOptions getKeyStore(Vertx vertx);
}
