package io.quarkus.it.opentracing;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TracedService {
    public String call() {
        return "Chained trace";
    }
}
