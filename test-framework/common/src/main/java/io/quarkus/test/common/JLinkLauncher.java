package io.quarkus.test.common;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Launcher for jlink-packaged Quarkus applications.
 * If an implementation of this class is found using the ServiceLoader mechanism, then it is used.
 * Otherwise {@link DefaultJLinkLauncher} is used.
 */
public interface JLinkLauncher extends ArtifactLauncher<JLinkLauncher.JLinkInitContext> {

    interface JLinkInitContext extends InitContext {

        Path imagePath();

        String launcherName();

        List<String> recordingArgs();

        List<String> postCloseCommand();

        Optional<Path> aotResultPath();

        String aotResultDescription();
    }
}
