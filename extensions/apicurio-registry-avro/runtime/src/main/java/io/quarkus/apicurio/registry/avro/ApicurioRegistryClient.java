package io.quarkus.apicurio.registry.avro;

import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.rest.client.VertxHttpClientProvider;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;

@Recorder
public class ApicurioRegistryClient {
    public void setup(RuntimeValue<Vertx> vertx) {
        RegistryClientFactory.setProvider(new VertxHttpClientProvider(vertx.getValue()));
    }
}
