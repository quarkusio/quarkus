package io.quarkus.smallrye.reactivemessaging.rabbitmq.runtime.devconsole;

import javax.enterprise.event.Observes;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;

import io.quarkus.runtime.StartupEvent;

@Singleton
public class RabbitHttpPortFinder {

    String httpPort;

    void collect(@Observes StartupEvent event, Config config) {
        httpPort = config.getValue("rabbitmq-http-port", String.class);
    }
}
