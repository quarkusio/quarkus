package io.quarkus.it.opentelemetry;

import javax.enterprise.context.ApplicationScoped;

import io.opentelemetry.extension.annotations.WithSpan;

@ApplicationScoped
public class TracedService {
    @WithSpan
    public String call() {
        return "Chained trace";
    }
}
