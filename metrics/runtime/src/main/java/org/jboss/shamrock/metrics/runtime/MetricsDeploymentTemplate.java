package org.jboss.shamrock.metrics.runtime;

import javax.enterprise.inject.spi.CDI;

import io.smallrye.metrics.MetricRegistries;
import io.smallrye.metrics.app.CounterImpl;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;

/**
 * Created by bob on 7/30/18.
 */
public class MetricsDeploymentTemplate {

    public void registerCounted(String name) {
        System.err.println("register: " + name);
        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.APPLICATION);
        registry.register(name, new CounterImpl());
    }

    public void createRegistries() {
        System.err.println("creating registries");
        MetricRegistries.get(MetricRegistry.Type.APPLICATION);
        MetricRegistries.get(MetricRegistry.Type.BASE);
        MetricRegistries.get(MetricRegistry.Type.VENDOR);
    }
}
