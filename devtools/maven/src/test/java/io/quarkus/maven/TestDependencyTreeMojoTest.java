package io.quarkus.maven;

public class TestDependencyTreeMojoTest extends DependencyTreeMojoTestBase {
    @Override
    protected String mode() {
        return "test";
    }
}
