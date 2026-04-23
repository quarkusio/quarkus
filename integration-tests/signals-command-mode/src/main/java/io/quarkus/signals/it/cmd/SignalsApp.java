package io.quarkus.signals.it.cmd;

import jakarta.inject.Inject;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.signals.Signal;

@QuarkusMain
public class SignalsApp implements QuarkusApplication {

    @Inject
    Signal<Cmd> signal;

    @Inject
    CmdReceivers receivers;

    @Override
    public int run(String... args) {
        // Test request with blocking receiver
        String result = signal.request(new Cmd("hello"), String.class);
        if (!"HELLO".equals(result)) {
            System.err.println("request failed: expected HELLO, got " + result);
            return 1;
        }
        if (receivers.blockingCount.get() == 0) {
            System.err.println("send failed: no receiver was invoked");
            return 1;
        }

        receivers.blockingCount.set(0);
        signal.publishUni(new Cmd("multi"))
                .await().indefinitely();
        if (receivers.blockingCount.get() == 0) {
            System.err.println("publish failed: not all receivers were invoked (blocking="
                    + receivers.blockingCount.get() + ")");
            return 1;
        }

        System.out.println("All signal tests passed");
        return 0;
    }
}
