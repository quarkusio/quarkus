package io.quarkus.smallrye.reactivemessaging.amqp;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class ConsumingBean {

    long last = -1;

    @Incoming("in")
    public synchronized void consume(long content) {
        last = content;
    }

    public long get() {
        return last;
    }

}
