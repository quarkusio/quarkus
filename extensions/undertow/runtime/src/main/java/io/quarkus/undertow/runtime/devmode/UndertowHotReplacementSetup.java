package io.quarkus.undertow.runtime.devmode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;
import io.quarkus.undertow.runtime.UndertowDeploymentRecorder;

public class UndertowHotReplacementSetup implements HotReplacementSetup {

    protected static final String META_INF_SERVICES = "META-INF/resources";

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        List<Path> resources = new ArrayList<>();
        for (Path i : context.getResourcesDir()) {
            Path resolved = i.resolve(META_INF_SERVICES);
            if (Files.exists(resolved)) {
                resources.add(resolved);
            }
        }
        UndertowDeploymentRecorder.setHotDeploymentResources(resources);
    }

    @Override
    public void close() {
        UndertowDeploymentRecorder.setHotDeploymentResources(null);
    }
}
