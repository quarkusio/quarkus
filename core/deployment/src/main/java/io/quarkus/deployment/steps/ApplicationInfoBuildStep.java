package io.quarkus.deployment.steps;

import static io.quarkus.deployment.ApplicationInfoUtil.APPLICATION_INFO_PROPERTIES;
import static io.quarkus.deployment.ApplicationInfoUtil.META_INF;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Properties;

import io.quarkus.deployment.ApplicationConfig;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;

public class ApplicationInfoBuildStep {

    private static final String PROPERTIES_FILE_TO_READ = META_INF + File.separator + APPLICATION_INFO_PROPERTIES;

    private static final String ARTIFACT_ID_KEY = "artifactId";
    private static final String VERSION_KEY = "version";
    private static final String UNSET_VALUE = "unset";

    @BuildStep
    public ApplicationInfoBuildItem create(ApplicationConfig applicationConfig) {
        final String userConfiguredName = applicationConfig.name;
        final String userConfiguredVersion = applicationConfig.version;

        final Properties applicationInfoProperties = getApplicationInfoProperties();

        return new ApplicationInfoBuildItem(
                useIfNotEmpty(userConfiguredName, applicationInfoProperties.getProperty(ARTIFACT_ID_KEY, UNSET_VALUE)),
                useIfNotEmpty(userConfiguredVersion, applicationInfoProperties.getProperty(VERSION_KEY, UNSET_VALUE)));
    }

    private Properties getApplicationInfoProperties() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ApplicationInfoBuildStep.class.getClassLoader();
        }
        try {
            final Properties p = new Properties();
            // work around #1477
            final Enumeration<URL> resources = cl == null ? ClassLoader.getSystemResources(PROPERTIES_FILE_TO_READ)
                    : cl.getResources(PROPERTIES_FILE_TO_READ);
            if (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                try (InputStream is = url.openStream()) {
                    try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        p.load(isr);
                    }
                }
            }
            return p;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read application-info.properties", e);
        }
    }

    private String useIfNotEmpty(String value, String defaultValue) {
        return (value != null) && !value.isEmpty() ? value : defaultValue;
    }
}
