package io.quarkus.apicurio.registry.common;

import org.jboss.logging.Logger;

import io.apicurio.registry.resolver.client.RegistryClientFacadeFactory;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;

@Recorder
public class ApicurioRegistryClient {

    private static final Logger log = Logger.getLogger(ApicurioRegistryClient.class);

    public void setup(RuntimeValue<Vertx> vertx) {
        RegistryClientFacadeFactory.vertx = vertx.getValue();
    }

    public void clearHttpClient() {
        try {
            RegistryClientFacadeFactory.vertx = null;
        } catch (Exception t) {
            log.error("Failed to clear Apicurio Http Client", t);
        }
    }
}
