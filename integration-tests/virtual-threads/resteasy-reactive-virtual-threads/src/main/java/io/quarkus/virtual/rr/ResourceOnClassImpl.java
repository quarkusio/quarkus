package io.quarkus.virtual.rr;

import jakarta.enterprise.context.RequestScoped;

import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.smallrye.common.annotation.RunOnVirtualThread;

@RequestScoped
@RunOnVirtualThread
public class ResourceOnClassImpl implements IResourceOnClass {

    private final Counter counter;

    ResourceOnClassImpl(Counter counter) {
        this.counter = counter;
    }

    public String testGet() {
        VirtualThreadsAssertions.assertEverything();
        return "hello-" + counter.increment();
    }

    public String testPost(String body) {
        VirtualThreadsAssertions.assertEverything();
        return body + "-" + counter.increment();
    }

}
