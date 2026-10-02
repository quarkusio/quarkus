package io.quarkus.gradle.application.internal.modelgen;

import java.util.List;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.gradle.api.tasks.Classpath;

public abstract class ConditionalDependencyCoordinatesValueSource
        implements ValueSource<List<String>, ConditionalDependencyCoordinatesValueSource.Parameters> {

    public interface Parameters extends ValueSourceParameters {

        @Classpath
        ConfigurableFileCollection getRuntimeArtifacts();
    }

    @Override
    public List<String> obtain() {
        return ConditionalDependencyResolver
                .conditionalDependencyCoordinates(getParameters().getRuntimeArtifacts().getFiles());
    }
}
