package io.quarkus.smallrye.opentracing.deployment;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.opentracing.Traced;

import io.opentracing.Tracer;

@ApplicationScoped
public class Service {

    @Inject
    Tracer tracer;

    @Inject
    EntityManager em;

    @Traced
    public void foo() {
    }

    // @Asynchronous methods (and their fallback methods) shouldn't be @Traced
    // because https://github.com/eclipse/microprofile-opentracing/issues/189
    @Asynchronous
    @Fallback(fallbackMethod = "fallback")
    @Timeout(value = 20L, unit = ChronoUnit.MILLIS)
    @Retry(delay = 10L, maxRetries = 2)
    public CompletionStage<String> faultTolerance() {
        tracer.buildSpan("ft").start().finish();
        throw new RuntimeException();
    }

    public CompletionStage<String> fallback() {
        tracer.buildSpan("fallback").start().finish();
        return CompletableFuture.completedFuture("fallback");
    }

    @Traced
    public List<Fruit> getFruits() {
        return em.createNamedQuery("Fruits.findAll", Fruit.class).getResultList();
    }
}
