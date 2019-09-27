package io.quarkus.undertow.deployment.devmode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.deployment.devmode.HotReplacementContext;
import io.quarkus.deployment.devmode.HotReplacementSetup;
import io.quarkus.undertow.runtime.UndertowDeploymentRecorder;

public class UndertowHotReplacementSetup implements HotReplacementSetup {
    // TODO: This is marked as protected but I don't see anyone else (other than this class) using it.
    // get rid of this if not used anywhere else or at least make it private
    protected static final String META_INF_SERVICES = "META-INF/resources";

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        List<Path> resources = new ArrayList<>();
        for (Path i : context.getResourcesDir()) {
            final String nameSeparator = i.getFileSystem().getSeparator();
            Path resolved = i.resolve("META-INF" + nameSeparator + "resources");
            if (Files.exists(resolved)) {
                resources.add(resolved);
            }
        }
        UndertowDeploymentRecorder.setHotDeploymentResources(resources);
    }

    @Override
    public void handleFailedInitialStart() {
    }

    @Override
    public void close() {
        UndertowDeploymentRecorder.setHotDeploymentResources(null);
    }
}
