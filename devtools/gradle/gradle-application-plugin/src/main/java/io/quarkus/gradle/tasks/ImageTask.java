
package io.quarkus.gradle.tasks;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.gradle.api.tasks.options.Option;

import io.quarkus.gradle.GradleUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;

public abstract class ImageTask extends QuarkusBuildProviderTask {

    public enum Builder {
        docker,
        jib,
        buildpack,
        openshift
    }

    Builder builder = Builder.docker;

    public ImageTask(QuarkusBuildConfiguration buildConfiguration, String description) {
        super(buildConfiguration, description);
    }

    @Option(option = "builder", description = "The container image extension to use for building the image (e.g. docker, jib, buildpack, openshift).")
    public void setBuilder(Builder builder) {
        this.builder = builder;
    }

    @Override
    public List<Dependency> forcedDependencies() {
        String quarkusVersion = GradleUtils.getQuarkusCoreVersion(getProject());
        return Arrays.asList(new ArtifactDependency("io.quarkus", "quarkus-container-image-" + builder.name(), null,
                ArtifactCoords.TYPE_JAR, quarkusVersion));
    }

    @Override
    public Map<String, String> forcedProperties() {
        return Map.of("quarkus.container-image.build", "true");
    }
}
