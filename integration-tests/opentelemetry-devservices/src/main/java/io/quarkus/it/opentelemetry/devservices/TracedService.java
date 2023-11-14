package io.quarkus.it.opentelemetry.devservices;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TracedService {
    public String call() {
        return "Chained trace";
    }
}
