package io.quarkus.maven;

public class BuildTreeMojoTest extends TreeMojoTestBase {

    @Override
    protected AbstractTreeMojo newTreeMojo() {
        return new BuildTreeMojo();
    }

    @Override
    protected String mojoName() {
        return "build-tree";
    }
}
