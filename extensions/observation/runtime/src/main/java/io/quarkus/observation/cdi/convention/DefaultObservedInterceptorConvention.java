package io.quarkus.observation.cdi.convention;

import io.micrometer.common.KeyValues;
import io.quarkus.observation.cdi.ObservedInterceptorContext;

public class DefaultObservedInterceptorConvention implements ObservedInterceptorConvention {

    private final String name;
    private final String contextualName;
    private final KeyValues lowCardinalityKeyValues;

    public DefaultObservedInterceptorConvention(String name, String contextualName,
            String codeNamespace, String codeFunction) {
        this.name = name;
        this.contextualName = contextualName;
        this.lowCardinalityKeyValues = KeyValues.of(
                "code.namespace", codeNamespace,
                "code.function", codeFunction);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getContextualName(ObservedInterceptorContext context) {
        return contextualName;
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ObservedInterceptorContext context) {
        return lowCardinalityKeyValues;
    }
}
