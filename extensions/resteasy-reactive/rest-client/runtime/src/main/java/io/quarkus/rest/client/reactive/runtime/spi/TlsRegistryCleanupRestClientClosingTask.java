package io.quarkus.rest.client.reactive.runtime.spi;

import org.jboss.resteasy.reactive.client.impl.ClientImpl;
import org.jboss.resteasy.reactive.client.impl.RestClientClosingTask;

import io.quarkus.rest.client.reactive.runtime.RestClientRecorder;

/**
 * Cleans up any the TLS config bookkeeping related that relates to a REST Client instance.
 * This is needed because otherwise we could run into a <a href="https://github.com/quarkusio/quarkus/issues/49871">memory
 * leak</a>
 */
public class TlsRegistryCleanupRestClientClosingTask implements RestClientClosingTask {

    @Override
    public void close(Context context) {
        ClientImpl clientImpl = context.baseTarget().getRestClient();
        RestClientRecorder.removeClientFromTlsConfigMap(clientImpl.getTlsConfigName(), clientImpl.getVertxHttpClient());
    }
}
