package org.jboss.shamrock.deployment;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import org.jboss.jandex.IndexView;
import org.jboss.shamrock.deployment.buildconfig.BuildConfig;

/**
 *
 */
public interface ArchiveContext {

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

    /**
     * @return The complete set of deployment descriptors
     */
    Set<Path> getDescriptors(String descriptor);
}
