package org.jboss.shamrock.deployment.builditem;

import java.util.Collection;
import java.util.Set;

import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.shamrock.deployment.ApplicationArchive;
import org.jboss.shamrock.deployment.ArchiveContext;

//temp class
public final class ApplicationArchivesBuildItem extends SimpleBuildItem {

    private final ArchiveContext archiveContext;

    public ApplicationArchivesBuildItem(ArchiveContext archiveContext) {
        this.archiveContext = archiveContext;
    }

    /**
     * Returns an {@link ApplicationArchive} that represents the classes and resources that are part of the current
     * project
     *
     * @return The root archive
     */
    public ApplicationArchive getRootArchive() {
        return archiveContext.getRootArchive();
    }

    /**
     * @return A set of all application archives, excluding the root archive
     */
    public Collection<ApplicationArchive> getApplicationArchives() {
        return archiveContext.getApplicationArchives();
    }

    /**
     * @return A set of all application archives, including the root archive
     */
    public Set<ApplicationArchive> getAllApplicationArchives() {
        return archiveContext.getAllApplicationArchives();
    }

}
