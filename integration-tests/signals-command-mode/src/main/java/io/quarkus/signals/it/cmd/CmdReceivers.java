package io.quarkus.signals.it.cmd;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Singleton;

import io.quarkus.signals.Receives;

@Singleton
public class CmdReceivers {

    final AtomicInteger blockingCount = new AtomicInteger();

    // Blocking signature → BLOCKING
    String toUpperCase(@Receives Cmd cmd) {
        blockingCount.incrementAndGet();
        return cmd.value().toUpperCase();
    }

}
