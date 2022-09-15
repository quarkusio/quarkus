package io.quarkus.smallrye.reactivemessaging.amqp;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class ConsumingBean {

    volatile long last = -1;

    @Incoming("in")
    public void consume(long content) {
        last = content;
    }

    public long get() {
        return last;
    }

}
