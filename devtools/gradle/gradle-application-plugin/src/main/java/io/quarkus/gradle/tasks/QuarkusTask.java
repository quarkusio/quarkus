package io.quarkus.gradle.tasks;

import java.util.Map;
import java.util.Properties;

import org.gradle.api.DefaultTask;

import io.quarkus.gradle.extension.QuarkusPluginExtension;
import io.quarkus.maven.dependency.ResolvedDependency;

public abstract class QuarkusTask extends DefaultTask {

    private QuarkusPluginExtension extension;

    QuarkusTask(String description) {
        setDescription(description);
        setGroup("quarkus");
    }

    QuarkusPluginExtension extension() {
        if (extension == null) {
            extension = getProject().getExtensions().findByType(QuarkusPluginExtension.class);
        }
        return extension;
    }

    protected Properties getBuildSystemProperties(ResolvedDependency appArtifact) {
        final Map<String, ?> properties = getProject().getProperties();
        final Properties realProperties = new Properties();
        for (Map.Entry<String, ?> entry : properties.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            if (key != null && value instanceof String && key.startsWith("quarkus.")) {
                realProperties.setProperty(key, (String) value);
            }
        }
        Map<String, String> quarkusBuildProperties = extension().getQuarkusBuildProperties().get();
        if (!quarkusBuildProperties.isEmpty()) {
            quarkusBuildProperties.entrySet().stream().filter(entry -> entry.getKey().startsWith("quarkus."))
                    .forEach(entry -> {
                        realProperties.put(entry.getKey(), entry.getValue());
                    });
        }
        realProperties.putIfAbsent("quarkus.application.name", appArtifact.getArtifactId());
        realProperties.putIfAbsent("quarkus.application.version", appArtifact.getVersion());
        return realProperties;
    }
}
