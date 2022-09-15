package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class ApplicationScopeObject {
    AtomicInteger counter = new AtomicInteger();

    public int getCount() {
        return counter.incrementAndGet();
    }
}
