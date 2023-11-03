package io.quarkus.smallrye.reactivemessaging.hotreload;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
public class SomeProcessor {

    @Incoming("my-source")
    @Outgoing("my-sink")
    public String process(int input) {
        return Long.toString(input * -1);
    }
}
