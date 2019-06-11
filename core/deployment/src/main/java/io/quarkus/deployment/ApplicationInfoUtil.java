package io.quarkus.deployment;

import static io.quarkus.deployment.builditem.ApplicationInfoBuildItem.UNSET_VALUE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Properties;

import io.quarkus.bootstrap.model.AppArtifact;

public final class ApplicationInfoUtil {

    public static final String META_INF = "META-INF";
    public static final String APPLICATION_INFO_PROPERTIES = "application-info.properties";
    public static final String PROPERTIES_FILE_TO_READ = META_INF + File.separator + APPLICATION_INFO_PROPERTIES;

    public static final String ARTIFACT_ID_KEY = "artifactId";
    public static final String VERSION_KEY = "version";

    private static String artifactId = UNSET_VALUE;
    private static String version = UNSET_VALUE;

    private ApplicationInfoUtil() {
    }

    public static String getArtifactId() {
        if (artifactId.equals(UNSET_VALUE)) {
            loadApplicationInfoProperties();
        }
        return artifactId;
    }

    public static String getVersion() {
        if (version.equals(UNSET_VALUE)) {
            loadApplicationInfoProperties();
        }
        return version;
    }

    public static void setArtifactId(String artifactId) {
        ApplicationInfoUtil.artifactId = artifactId;
    }

    public static void setVersion(String version) {
        ApplicationInfoUtil.version = version;
    }

    private static void loadApplicationInfoProperties() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ApplicationInfoUtil.class.getClassLoader();
        }
        try {
            final Properties properties = new Properties();
            // work around #1477
            final Enumeration<URL> resources = cl == null ? ClassLoader.getSystemResources(PROPERTIES_FILE_TO_READ)
                    : cl.getResources(PROPERTIES_FILE_TO_READ);
            if (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                try (InputStream is = url.openStream()) {
                    try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        properties.load(isr);
                    }
                }
            }
            setVersion(properties.getProperty(VERSION_KEY, UNSET_VALUE));
            setArtifactId(properties.getProperty(ARTIFACT_ID_KEY, UNSET_VALUE));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read application-info.properties", e);
        }
    }

    // these properties are used as default values for ApplicationInfoBuildItem
    public static void writeApplicationInfoProperties(AppArtifact appArtifact, Path appClassesDir) {
        Properties properties = new Properties();
        if (appArtifact != null) {
            properties.setProperty(ARTIFACT_ID_KEY, appArtifact.getArtifactId());
            setArtifactId(appArtifact.getArtifactId());
            properties.setProperty(VERSION_KEY, appArtifact.getVersion());
            setArtifactId(appArtifact.getVersion());
        }
        try {
            appClassesDir.resolve(META_INF).toFile().mkdirs();
            File file = appClassesDir.resolve(META_INF).resolve(APPLICATION_INFO_PROPERTIES).toFile();
            properties.store(new FileOutputStream(file), "Generated file; do not edit manually");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
