package io.quarkus.registry.client.spi;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;

public interface RegistryClientEnvironment {

    MessageWriter log();

    MavenArtifactResolver resolver();
}
