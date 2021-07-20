package org.acme.ext.deployment;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class AcmeExtProcessor {

    private static final String FEATURE = "acme-ext";

    @BuildStep
    FeatureBuildItem feature(ApplicationArchivesBuildItem appArchivesBuildItem,
            LaunchModeBuildItem launchModeBuildItem) {
        Path p = appArchivesBuildItem.getRootArchive().getChildPath("resources.txt");
        if(p == null) {
            throw new IllegalStateException("Failed to locate resources.txt in the project's resources");
        }

        final Path output = p.getParent().getParent().getParent().resolve(launchModeBuildItem.getLaunchMode() + "-" + p.getFileName());
        try {
            Files.copy(p, output);
        } catch(IOException e) {
            throw new IllegalStateException("Failed to copy " + p + " to " + output);
        }
        return new FeatureBuildItem(FEATURE);
    }
}
