package io.quarkus.virtual.vertx;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;

@ApplicationScoped
public class EventBusConsumer {

    public static final List<String> ONE_WAY = new CopyOnWriteArrayList<>();

    @ConsumeEvent("one-way")
    @RunOnVirtualThread
    void receive(String m) {
        VirtualThreadsAssertions.assertEverything();
        ONE_WAY.add(m);
    }

    @ConsumeEvent("request-reply")
    @RunOnVirtualThread
    String process(String m) {
        VirtualThreadsAssertions.assertEverything();
        return m.toUpperCase();
    }

}
