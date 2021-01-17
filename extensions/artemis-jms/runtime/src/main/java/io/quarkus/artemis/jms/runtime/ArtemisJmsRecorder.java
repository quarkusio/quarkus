package io.quarkus.artemis.jms.runtime;

import java.util.function.Supplier;

import javax.jms.ConnectionFactory;

import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;

import io.quarkus.artemis.core.runtime.ArtemisRuntimeConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ArtemisJmsRecorder {

    public Supplier<ConnectionFactory> getConnectionFactorySupplier(ArtemisRuntimeConfig config) {
        return new Supplier<ConnectionFactory>() {
            @Override
            public ConnectionFactory get() {
                return new ActiveMQJMSConnectionFactory(config.url, config.username.orElse(null), config.password.orElse(null));
            }
        };
    }
}
