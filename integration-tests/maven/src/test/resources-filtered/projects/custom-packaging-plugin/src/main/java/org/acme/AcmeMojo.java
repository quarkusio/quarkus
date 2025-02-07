package org.acme;

@Mojo(name = "hello",
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresProject = true,
        defaultPhase = LifecyclePhase.COMPILE)
public class AcmeMojo extends org.apache.maven.plugin.AbstractMojo {

    @Override
    public void execute() {
    }
}
