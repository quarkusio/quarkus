package org.jboss.shamrock.deployment;

import java.util.Collection;
import java.util.Set;

import org.jboss.jandex.IndexView;
import org.jboss.shamrock.deployment.buildconfig.BuildConfig;

/**
 * Represents the input to the shamrock build process.
 */
public interface ArchiveContext {

    /**
     * Returns an {@link ApplicationArchive} that represents the classes and resources that are part of the current
     * project
     *
     * @return The root archive
     */
    ApplicationArchive getRootArchive();

    /**
     * @return A set of all application archives, excluding the root archive
     */
    Collection<ApplicationArchive> getApplicationArchives();

    /**
     * @return A set of all application archives, including the root archive
     */
    Set<ApplicationArchive> getAllApplicationArchives();

    /**
     * @return An index of all the application archives
     */
    IndexView getCombinedIndex();

    /**
     * @return The configuration of the application
     */
    BuildConfig getBuildConfig();

}
