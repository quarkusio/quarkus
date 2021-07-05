package io.quarkus.test.common;

import java.nio.file.Path;

/**
 * If an implementation of this class is found using the ServiceLoader mechanism, then it is used.
 * Otherwise {@link DefaultJarLauncher} is used
 */
public interface JarArtifactLauncher extends ArtifactLauncher<JarArtifactLauncher.JarInitContext> {

    interface JarInitContext extends InitContext {

        String argLine();

        Path jarPath();
    }
}
