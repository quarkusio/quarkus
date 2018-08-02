package org.jboss.shamrock.deployment;

import java.nio.file.Path;
import java.util.List;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.deployment.buildconfig.BuildConfig;

public interface ArchiveContext {

    /**
     * The combined index, which includes the current application as well as
     * any indexed jars on the build path.
     *
     * @return
     */
    IndexView getIndex();

    Path getArchiveRoot();

    BuildConfig getBuildConfig();

}
