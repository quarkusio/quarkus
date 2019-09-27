package io.quarkus.resteasy.deployment.devmode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.deployment.devmode.HotReplacementContext;
import io.quarkus.deployment.devmode.HotReplacementSetup;
import io.quarkus.resteasy.runtime.standalone.ResteasyStandaloneRecorder;

public class ResteasyHotReplacementSetup implements HotReplacementSetup {

    public static final String META_INF_SERVICES = "META-INF/resources";

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        List<Path> resources = new ArrayList<>();
        for (Path i : context.getResourcesDir()) {
            Path resolved = i.resolve(META_INF_SERVICES);
            if (Files.exists(resolved)) {
                resources.add(resolved);
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
