package io.quarkus.opentelemetry.observation.cdi.convention;

import io.micrometer.common.KeyValues;
import io.quarkus.opentelemetry.observation.cdi.ObservedInterceptorContext;

public class DefaultObservedInterceptorConvention implements ObservedInterceptorConvention {

    private final String name;

    public DefaultObservedInterceptorConvention(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getContextualName(ObservedInterceptorContext context) {
        return context.getInvocationContext().getMethod().getDeclaringClass().getSimpleName()
                + "#" + context.getInvocationContext().getMethod().getName();
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ObservedInterceptorContext context) {
        return KeyValues.of(
                "code.namespace", context.getInvocationContext().getMethod().getDeclaringClass().getName(),
                "code.function", context.getInvocationContext().getMethod().getName());
    }
}
