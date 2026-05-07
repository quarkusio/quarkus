package io.quarkus.signals.it;

import jakarta.inject.Inject;

import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;

public class HelloSignals {

    @Inject
    Signal<HelloMessage> signal;

    HelloMessage hello(@Receives HelloName name) {
        HelloMessage msg = new HelloMessage("Hello " + name.name() + "!");
        signal.publish(msg);
        return msg;
    }

}
