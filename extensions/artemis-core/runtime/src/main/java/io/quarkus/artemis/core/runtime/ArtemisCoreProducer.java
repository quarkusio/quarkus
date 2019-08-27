package io.quarkus.artemis.core.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ServerLocator;

@ApplicationScoped
public class ArtemisCoreProducer {

    private ArtemisRuntimeConfig config;

    @Produces
    public ServerLocator produceServerLocator() throws Exception {
        return ActiveMQClient.createServerLocator(config.url);
    }

    public ArtemisRuntimeConfig getConfig() {
        return config;
    }

    public void setConfig(ArtemisRuntimeConfig config) {
        this.config = config;
    }
}
