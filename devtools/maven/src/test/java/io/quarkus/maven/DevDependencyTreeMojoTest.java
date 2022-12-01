package io.quarkus.maven;

public class DevDependencyTreeMojoTest extends DependencyTreeMojoTestBase {
    @Override
    protected String mode() {
        return "dev";
    }
}
