package io.quarkus.it.observation.micrometer.opentelemetry;

import static io.micrometer.common.KeyValue.of;

import jakarta.enterprise.context.ApplicationScoped;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import io.quarkus.arc.properties.IfBuildProperty;

@ApplicationScoped
@IfBuildProperty(name = "test.observation.customizations", stringValue = "true", enableIfMissing = false)
public class CustomObservationFilter implements ObservationFilter {

    @Override
    public Observation.Context map(Observation.Context context) {
        context.addLowCardinalityKeyValue(of("filtered.by", "custom-filter"));
        return context;
    }
}
