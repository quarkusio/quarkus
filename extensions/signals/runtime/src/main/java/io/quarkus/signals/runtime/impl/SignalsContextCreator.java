package io.quarkus.signals.runtime.impl;

import java.util.List;
import java.util.Map;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.signals.runtime.impl.SignalsRecorder.SignalsContext;

public class SignalsContextCreator implements BeanCreator<SignalsContext> {

    public static final String RECEIVER_CLASSES = "receiverClasses";
    public static final String ENRICHER_IDS = "enricherIds";
    public static final String INTERCEPTOR_IDS = "interceptorIds";

    @Override
    public SignalsContext create(SyntheticCreationalContext<SignalsContext> context) {
        Map<String, Object> params = context.getParams();
        return new SignalsContext(List.of((String[]) params.get(RECEIVER_CLASSES)),
                List.of((String[]) params.get(ENRICHER_IDS)), List.of((String[]) params.get(INTERCEPTOR_IDS)));
    }
}
