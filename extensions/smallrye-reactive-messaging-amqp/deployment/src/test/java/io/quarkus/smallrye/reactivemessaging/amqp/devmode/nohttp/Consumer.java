package io.quarkus.smallrye.reactivemessaging.amqp.devmode.nohttp;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class Consumer {
    private static final Logger log = Logger.getLogger(Consumer.class);

    @Incoming("in")
    public void consume(long content) {
        log.info(content * 1);
    }
}
