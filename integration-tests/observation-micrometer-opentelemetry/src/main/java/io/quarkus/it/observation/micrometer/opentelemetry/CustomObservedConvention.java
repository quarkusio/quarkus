package io.quarkus.it.observation.micrometer.opentelemetry;

import jakarta.enterprise.context.ApplicationScoped;

import io.micrometer.common.KeyValues;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.observation.cdi.ObservedInterceptorContext;
import io.quarkus.observation.cdi.convention.ObservedInterceptorConvention;

@ApplicationScoped
@IfBuildProperty(name = "test.observation.customizations", stringValue = "true", enableIfMissing = false)
public class CustomObservedConvention implements ObservedInterceptorConvention {

    @Override
    public String getName() {
        return "custom.observed";
    }

    @Override
    public String getContextualName(ObservedInterceptorContext context) {
        return "custom-" + context.getInvocationContext().getMethod().getName();
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ObservedInterceptorContext context) {
        return KeyValues.of(
                "code.function", context.getInvocationContext().getMethod().getName(),
                "code.namespace", context.getInvocationContext().getMethod().getDeclaringClass().getName(),
                "convention", "custom");
    }
}
