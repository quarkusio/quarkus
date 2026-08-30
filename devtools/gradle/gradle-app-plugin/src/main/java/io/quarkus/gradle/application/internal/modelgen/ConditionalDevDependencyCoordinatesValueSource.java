package io.quarkus.gradle.application.internal.modelgen;

import java.util.List;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.gradle.api.tasks.Classpath;

public abstract class ConditionalDevDependencyCoordinatesValueSource
        implements ValueSource<List<String>, ConditionalDevDependencyCoordinatesValueSource.Parameters> {

    public interface Parameters extends ValueSourceParameters {

        @Classpath
        ConfigurableFileCollection getRuntimeArtifacts();
    }

    @Override
    public List<String> obtain() {
        return ConditionalDependencyResolver
                .conditionalDevDependencyCoordinates(getParameters().getRuntimeArtifacts().getFiles());
    }
}
