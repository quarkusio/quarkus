package io.quarkus.maven;

class AddExtensionsMojoTest extends AddExtensionMojoTest {

    @Override
    protected AddExtensionMojo getMojo() {
        return new AddExtensionsMojo();
    }
}
