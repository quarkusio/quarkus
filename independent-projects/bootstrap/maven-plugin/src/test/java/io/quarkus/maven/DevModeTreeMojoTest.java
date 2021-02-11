package io.quarkus.maven;

public class DevModeTreeMojoTest extends TreeMojoTestBase {

    @Override
    protected AbstractTreeMojo newTreeMojo() {
        return new DevModeTreeMojo();
    }

    @Override
    protected String mojoName() {
        return "dev-mode-tree";
    }
}
