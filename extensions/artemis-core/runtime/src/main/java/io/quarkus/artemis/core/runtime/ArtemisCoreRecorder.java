package io.quarkus.artemis.core.runtime;

import java.util.function.Supplier;

import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ServerLocator;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ArtemisCoreRecorder {

    final ArtemisRuntimeConfig config;

    public ArtemisCoreRecorder(ArtemisRuntimeConfig config) {
        this.config = config;
    }

    public Supplier<ServerLocator> getServerLocatorSupplier() {
        return new Supplier<ServerLocator>() {
            @Override
            public ServerLocator get() {
                try {
                    return ActiveMQClient.createServerLocator(config.url);
                } catch (Exception e) {
                    throw new RuntimeException("Could not create ServerLocator", e);
                }
            }
        };
    }
}
