package io.quarkus.opentelemetry.runtime.tracing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.StaticInit;

@Recorder
public class TracerRecorder {

    public static final Set<String> dropNonApplicationUriTargets = new HashSet<>();
    public static final Set<String> dropStaticResourceTargets = new HashSet<>();

    @StaticInit
    public void setupSampler(
            List<String> dropNonApplicationUris,
            List<String> dropStaticResources) {
        dropNonApplicationUriTargets.addAll(dropNonApplicationUris);
        dropStaticResourceTargets.addAll(dropStaticResources);
    }

}
