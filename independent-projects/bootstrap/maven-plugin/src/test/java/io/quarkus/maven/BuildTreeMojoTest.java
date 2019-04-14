package io.quarkus.maven;

import io.quarkus.maven.BuildTreeMojo;

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
