package io.quarkus.deployment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import io.quarkus.bootstrap.model.AppArtifact;

public final class ApplicationInfoUtil {

    public static final String APPLICATION_INFO_PROPERTIES = "application-info.properties";
    public static final String META_INF = "META-INF";

    private ApplicationInfoUtil() {
    }

    // these properties are used as default values for ApplicationInfoBuildItem
    public static void writeApplicationInfoProperties(AppArtifact appArtifact, Path appClassesDir) {
        Properties properties = new Properties();
        properties.setProperty("artifactId", appArtifact.getArtifactId());
        properties.setProperty("version", appArtifact.getVersion());
        try {
            appClassesDir.resolve(META_INF).toFile().mkdir();
            File file = appClassesDir.resolve(META_INF).resolve(APPLICATION_INFO_PROPERTIES).toFile();
            properties.store(new FileOutputStream(file), "Generated file; do not edit manually");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
