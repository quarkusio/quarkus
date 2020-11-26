package io.quarkus.rest.server.test.resource.basic.resource;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ApplicationScopeObject {
    AtomicInteger counter = new AtomicInteger();

    public int getCount() {
        return counter.incrementAndGet();
    }
}
