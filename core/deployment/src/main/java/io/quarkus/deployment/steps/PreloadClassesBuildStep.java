package io.quarkus.deployment.steps;

import static io.quarkus.runtime.PreloadClassesRecorder.QUARKUS_GENERATED_PRELOAD_CLASSES_FILE;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.PreloadClassBuildItem;
import io.quarkus.deployment.builditem.PreloadClassesEnabledBuildItem;
import io.quarkus.runtime.PreloadClassesRecorder;

public class PreloadClassesBuildStep {
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void preInit(Optional<PreloadClassesEnabledBuildItem> preload, PreloadClassesRecorder recorder) {
        if (!preload.isPresent())
            return;
        recorder.invokePreloadClasses(preload.get().doInitialize());
    }

    @BuildStep
    public GeneratedResourceBuildItem registerPreInitClasses(List<PreloadClassBuildItem> items) {
        if (items == null || items.isEmpty())
            return null;
        // ensure unique & sorted
        final String names = items.stream().map(PreloadClassBuildItem::getClassName).sorted().distinct()
                .map(s -> s.concat(System.lineSeparator())).collect(Collectors.joining());
        return new GeneratedResourceBuildItem("META-INF/" + QUARKUS_GENERATED_PRELOAD_CLASSES_FILE,
                names.getBytes(StandardCharsets.UTF_8));
    }
}
