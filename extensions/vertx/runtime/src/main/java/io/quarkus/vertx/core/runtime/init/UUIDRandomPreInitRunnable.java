package io.quarkus.vertx.core.runtime.init;

import java.util.UUID;

public class UUIDRandomPreInitRunnable implements Runnable {

    @Override
    public void run() {
        // initialize the SecureRandom in UUID as a pre-init task as it's quite slow
        UUID.randomUUID();
    }
}