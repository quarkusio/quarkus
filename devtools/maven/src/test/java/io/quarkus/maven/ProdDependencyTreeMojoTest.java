package io.quarkus.maven;

public class ProdDependencyTreeMojoTest extends DependencyTreeMojoTestBase {
    @Override
    protected String mode() {
        return "prod";
    }
}
