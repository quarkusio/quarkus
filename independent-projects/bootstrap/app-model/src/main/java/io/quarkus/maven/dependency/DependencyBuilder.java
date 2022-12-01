package io.quarkus.maven.dependency;

public class DependencyBuilder extends AbstractDependencyBuilder<DependencyBuilder, Dependency> {

    public static DependencyBuilder newInstance() {
        return new DependencyBuilder();
    }

    @Override
    public Dependency build() {
        return new ArtifactDependency(this);
    }
}
