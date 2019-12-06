package io.quarkus.artemis.jms.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.jms.ConnectionFactory;

import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;

import io.quarkus.arc.DefaultBean;
import io.quarkus.artemis.core.runtime.ArtemisRuntimeConfig;

@ApplicationScoped
public class ArtemisJmsProducer {

    private ArtemisRuntimeConfig config;

    @Produces
    @ApplicationScoped
    @DefaultBean
    public ConnectionFactory connectionFactory() {
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
