package io.quarkus.it.opentelemetry;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TracedService {
    public String call() {
        return "Chained trace";
    }
}
