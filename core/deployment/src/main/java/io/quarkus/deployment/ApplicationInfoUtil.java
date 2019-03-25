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

    public static final String GROUP_ID_KEY = "groupId";
    public static final String ARTIFACT_ID_KEY = "artifactId";
    public static final String VERSION_KEY = "version";
    public static final String FINAL_NAME_KEY = "finalName";
    public static final String BASE_DIR_KEY = "baseDir";
    public static final String WIRING_CLASSES_DIR_KEY = "wiringClassesDir";

    private ApplicationInfoUtil() {
    }

    // these properties are used as default values for ApplicationInfoBuildItem
    public static void writeApplicationInfoProperties(AppArtifact appArtifact,
            Path baseDir, Path appClassesDir, Path wiringClassesDir,
            String finalName) {
        Properties properties = new Properties();
        if (appArtifact != null) {
            properties.setProperty("groupId", appArtifact.getGroupId());
            properties.setProperty("artifactId", appArtifact.getArtifactId());
            properties.setProperty("version", appArtifact.getVersion());
        }
        if (baseDir != null) {
            properties.setProperty("baseDir", baseDir.toAbsolutePath().toString());
        }
        if (wiringClassesDir != null) {
            properties.setProperty("wiringClassesDir", wiringClassesDir.toAbsolutePath().toString());
        }
        if (finalName != null) {
            properties.setProperty("finalName", finalName);
        }
        try {
            appClassesDir.resolve(META_INF).toFile().mkdir();
            File file = appClassesDir.resolve(META_INF).resolve(APPLICATION_INFO_PROPERTIES).toFile();
            properties.store(new FileOutputStream(file), "Generated file; do not edit manually");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
