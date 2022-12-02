package io.quarkus.it.opentelemetry;

import javax.enterprise.context.ApplicationScoped;

import io.opentelemetry.instrumentation.annotations.WithSpan;

@ApplicationScoped
public class TracedService {
    @WithSpan
    public String call() {
        return "Chained trace";
    }
}
