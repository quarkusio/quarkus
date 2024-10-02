package io.quarkus.it.opentelemetry;

import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.instrumentation.annotations.WithSpan;

@ApplicationScoped
public class TracedService {

    private static final Logger LOG = LoggerFactory.getLogger(TracedService.class);

    @WithSpan
    public String call() {
        LOG.info("Chained trace called");
        return "Chained trace";
    }
}
