package io.quarkus.gradle.application.internal.modelgen;

import java.util.List;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.gradle.api.tasks.Input;

public abstract class SatisfiedConditionalDependencyCoordinatesValueSource
        implements ValueSource<List<String>, SatisfiedConditionalDependencyCoordinatesValueSource.Parameters> {

    public interface Parameters extends ValueSourceParameters {

        @Input
        ListProperty<String> getRuntimeComponentKeys();

        @Input
        ListProperty<String> getConditionalArtifactRecords();
    }

    @Override
    public List<String> obtain() {
        return ConditionalDependencyResolver.satisfiedConditionalDependencyCoordinates(
                getParameters().getRuntimeComponentKeys().get(),
                getParameters().getConditionalArtifactRecords().get());
    }
}
