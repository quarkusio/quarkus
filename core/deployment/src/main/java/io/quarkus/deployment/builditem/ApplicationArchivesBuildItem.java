package io.quarkus.deployment.builditem;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.ApplicationArchive;

//temp class
public final class ApplicationArchivesBuildItem extends SimpleBuildItem {

    private final ApplicationArchive root;
    private final Collection<ApplicationArchive> applicationArchives;
    private Set<ApplicationArchive> allArchives;

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
        if (allArchives == null) {
            HashSet<ApplicationArchive> ret = new HashSet<>(applicationArchives);
            ret.add(root);
            allArchives = Collections.unmodifiableSet(ret);
        }
        return allArchives;
    }

    /**
     * Returns the archive that contains the given class name, or null if the class cannot be found
     *
     * @param className The class name
     * @return The application archive
     */
    public ApplicationArchive containingArchive(String className) {
        DotName name = DotName.createSimple(className);
        if (root.getIndex().getClassByName(name) != null) {
            return root;
        }
        for (ApplicationArchive i : applicationArchives) {
            if (i.getIndex().getClassByName(name) != null) {
                return i;
            }
        }
        return null;
    }

}
