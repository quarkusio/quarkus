package io.quarkus.maven;

public class DevDependencyTreeMojoTest extends BasicDependencyTreeTestBase {
    @Override
    protected String mode() {
        return "dev";
    }
}
