package io.quarkus.resteasy.runtime.devmode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;
import io.quarkus.resteasy.runtime.standalone.ResteasyStandaloneRecorder;

public class ResteasyHotReplacementSetup implements HotReplacementSetup {

    public static final String META_INF_RESOURCES = "META-INF/resources";

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        List<Path> resources = new ArrayList<>();
        for (Path resourceDir : context.getResourcesDir()) {
            Path resource = resourceDir.resolve(META_INF_RESOURCES);
            if (Files.exists(resource)) {
                resources.add(resource);
            }
        }
        ResteasyStandaloneRecorder.setHotDeploymentResources(resources);
    }

    @Override
    public void handleFailedInitialStart() {
    }

    @Override
    public void close() {
        ResteasyStandaloneRecorder.setHotDeploymentResources(null);
    }
}
