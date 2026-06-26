package io.quarkus.it.observation.micrometer.opentelemetry;

import jakarta.enterprise.context.ApplicationScoped;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationPredicate;
import io.quarkus.arc.properties.IfBuildProperty;

@ApplicationScoped
@IfBuildProperty(name = "test.observation.customizations", stringValue = "true", enableIfMissing = false)
public class CustomObservationPredicate implements ObservationPredicate {

    @Override
    public boolean test(String name, Observation.Context context) {
        return !"ignored.operation".equals(name);
    }
}
