package io.quarkus.apicurio.registry.common;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.logging.Logger;

import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.rest.client.VertxHttpClientProvider;
import io.apicurio.rest.client.spi.ApicurioHttpClientFactory;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;

@Recorder
public class ApicurioRegistryClient {

    private static final Logger log = Logger.getLogger(ApicurioRegistryClient.class);

    public void setup(RuntimeValue<Vertx> vertx) {
        RegistryClientFactory.setProvider(new VertxHttpClientProvider(vertx.getValue()));
    }

    public void clearHttpClient() {
        try {
            Field providerReference = ApicurioHttpClientFactory.class.getDeclaredField("providerReference");
            providerReference.setAccessible(true);
            AtomicReference ref = (AtomicReference) providerReference.get(null);
            ref.set(null);
        } catch (NoSuchFieldException | IllegalAccessException t) {
            log.error("Failed to clear Apicurio Http Client provider", t);
        }
    }
}