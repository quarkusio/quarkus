package io.quarkus.bootstrap.resolver.maven;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.aether.spi.artifact.transformer.ArtifactTransformer;
import org.eclipse.sisu.Priority;

@Singleton
@Named
@Priority(100)
public class BootstrapArtifactTransformer implements ArtifactTransformer {
}
