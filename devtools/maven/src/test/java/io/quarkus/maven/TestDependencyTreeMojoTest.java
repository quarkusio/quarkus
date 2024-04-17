package io.quarkus.maven;

public class TestDependencyTreeMojoTest extends BasicDependencyTreeTestBase {
    @Override
    protected String mode() {
        return "test";
    }
}
