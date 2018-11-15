package org.jboss.shamrock.deployment.builditem;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.shamrock.deployment.ApplicationArchive;

//temp class
public final class ApplicationArchivesBuildItem extends SimpleBuildItem {

    private final ApplicationArchive root;
    private final Collection<ApplicationArchive> applicationArchives;

    public ApplicationArchivesBuildItem(ApplicationArchive root, Collection<ApplicationArchive> applicationArchives) {
        this.root = root;
        this.applicationArchives = applicationArchives;
    }


    /**
     * Returns an {@link ApplicationArchive} that represents the classes and resources that are part of the current
     * project
     *
     * @return The root archive
     */
    public ApplicationArchive getRootArchive() {
        return root;
    }

    /**
     * @return A set of all application archives, excluding the root archive
     */
    public Collection<ApplicationArchive> getApplicationArchives() {
        return applicationArchives;
    }

    /**
     * @return A set of all application archives, including the root archive
     */
    public Set<ApplicationArchive> getAllApplicationArchives() {
        HashSet<ApplicationArchive> ret = new HashSet<>(applicationArchives);
        ret.add(root);
        return Collections.unmodifiableSet(ret);
    }

}
