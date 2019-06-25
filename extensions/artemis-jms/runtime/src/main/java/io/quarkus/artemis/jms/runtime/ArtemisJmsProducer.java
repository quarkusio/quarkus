package io.quarkus.artemis.jms.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.jms.ConnectionFactory;

import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;

import io.quarkus.artemis.core.runtime.ArtemisRuntimeConfig;

@ApplicationScoped
public class ArtemisJmsProducer {

    private ArtemisRuntimeConfig config;

    @Produces
    public ConnectionFactory producesConnectionFactory() {
        return new ActiveMQJMSConnectionFactory(config.url,
                config.username.orElse(null), config.password.orElse(null));
    }

    public ArtemisRuntimeConfig getConfig() {
        return config;
    }

    public void setConfig(ArtemisRuntimeConfig config) {
        this.config = config;
    }
}
