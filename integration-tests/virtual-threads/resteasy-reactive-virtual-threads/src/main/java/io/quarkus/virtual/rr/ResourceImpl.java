package io.quarkus.virtual.rr;

import jakarta.enterprise.context.RequestScoped;

import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.smallrye.common.annotation.RunOnVirtualThread;

@RequestScoped
public class ResourceImpl implements IResource {

    private final Counter counter;

    ResourceImpl(Counter counter) {
        this.counter = counter;
    }

    @RunOnVirtualThread
    public String testGet() {
        VirtualThreadsAssertions.assertEverything();
        return "hello-" + counter.increment();
    }

    @RunOnVirtualThread
    public String testPost(String body) {
        VirtualThreadsAssertions.assertEverything();
        return body + "-" + counter.increment();
    }

}
