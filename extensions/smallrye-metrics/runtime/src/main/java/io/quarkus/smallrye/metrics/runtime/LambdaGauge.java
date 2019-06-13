package io.quarkus.smallrye.metrics.runtime;

import java.util.concurrent.Callable;

import org.eclipse.microprofile.metrics.Gauge;

public class LambdaGauge implements Gauge<Number> {

    public LambdaGauge(Callable<Number> callable) {
        this.callable = callable;
    }

    @Override
    public Number getValue() {
        try {
            return this.callable.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private final Callable<Number> callable;
}
