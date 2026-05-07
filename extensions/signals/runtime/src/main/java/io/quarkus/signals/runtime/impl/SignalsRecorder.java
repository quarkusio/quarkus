package io.quarkus.signals.runtime.impl;

import java.util.List;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class SignalsRecorder {

    public Supplier<Object> createContext(List<String> receiversClasses, List<String> orderedEnricherIds,
            List<String> orderedInterceptorIds) {
        return new Supplier<Object>() {

            @Override
            public Object get() {
                return new SignalsContext(receiversClasses, orderedEnricherIds, orderedInterceptorIds);
            }
        };
    }

    public record SignalsContext(List<String> receiversClasses, List<String> orderedEnricherIds,
            List<String> orderedInterceptorIds) {
    }
}
