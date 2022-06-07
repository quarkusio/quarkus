package io.quarkus.test.junit.launcher;

import static io.quarkus.test.junit.ArtifactTypeUtil.isAppCds;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.Config;

public class AppCdsLauncherProvider extends JarLauncherProvider {

    @Override
    public boolean supportsArtifactType(String type) {
        return isAppCds(type);
    }

    @Override
    protected String getPath(CreateContext context) {
        return context.quarkusArtifactProperties().getProperty("metadata.jar-result-path");
    }

    @Override
    protected List<String> argLineValue(CreateContext context, Config config) {
        List<String> argLines = new ArrayList<>();
        argLines.addAll(super.argLineValue(context, config));
        argLines.add("-XX:SharedArchiveFile=" + context.quarkusArtifactProperties().getProperty("path"));
        return argLines;
    }
}
