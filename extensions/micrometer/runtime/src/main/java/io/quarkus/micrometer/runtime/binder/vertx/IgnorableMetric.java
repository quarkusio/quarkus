package io.quarkus.micrometer.runtime.binder.vertx;

public interface IgnorableMetric {

    void markAsIgnored();

    boolean isIgnored();
}
