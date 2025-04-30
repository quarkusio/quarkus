package io.quarkus.maven;

public class ProdDependencyTreeMojoTest extends BasicDependencyTreeTestBase {
    @Override
    protected String mode() {
        return "prod";
    }
}
