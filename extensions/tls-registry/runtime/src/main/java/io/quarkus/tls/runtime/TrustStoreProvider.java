package io.quarkus.tls.runtime;

import io.vertx.core.Vertx;

/**
 * An interface for providing {@link TrustStoreAndTrustOptions} from CDI beans at runtime.
 */
@FunctionalInterface
public interface TrustStoreProvider {

    /**
     * Returns the truststore and options to be used for [re]loading the state of a TLS configuration
     *
     * @param vertx the managed vertx instance
     * @return the truststore and options
     */
    TrustStoreAndTrustOptions getTrustStore(Vertx vertx);
}
