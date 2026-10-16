package io.quarkus.signals.runtime.impl;

import java.util.List;

public class SignalsRecorder {

    public record SignalsContext(List<String> receiversClasses, List<String> orderedEnricherIds,
            List<String> orderedInterceptorIds) {
    }
}
