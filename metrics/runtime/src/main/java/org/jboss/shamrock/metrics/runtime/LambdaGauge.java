package org.jboss.shamrock.metrics.runtime;

import java.util.concurrent.Callable;

import org.eclipse.microprofile.metrics.Gauge;

/**
 * Created by bob on 7/31/18.
 */
public class LambdaGauge implements Gauge {

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
