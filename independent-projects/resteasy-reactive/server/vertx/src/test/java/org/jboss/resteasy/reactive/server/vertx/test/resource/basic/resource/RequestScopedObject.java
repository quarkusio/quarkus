package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.enterprise.context.RequestScoped;
import java.util.concurrent.atomic.AtomicInteger;

@RequestScoped
public class RequestScopedObject {

    AtomicInteger count = new AtomicInteger();

    public int getCount() {
        return count.incrementAndGet();
    }
}
