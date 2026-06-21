package io.quarkus.deployment.builditem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.ApplicationArchive;

public final class ApplicationArchivesBuildItem extends SimpleBuildItem {

    private final ApplicationArchive root;
    private final List<ApplicationArchive> applicationArchives;
    private final List<ApplicationArchive> allArchives;

    public ApplicationArchivesBuildItem(ApplicationArchive root, List<ApplicationArchive> applicationArchives) {
        this.root = root;
        this.applicationArchives = Collections.unmodifiableList(applicationArchives);

        List<ApplicationArchive> all = new ArrayList<>(applicationArchives.size() + 1);
        all.add(root);
        all.addAll(applicationArchives);

        this.allArchives = Collections.unmodifiableList(all);
    }

    /**
     * Returns an {@link ApplicationArchive} that represents the classes and resources that are part of the current
     * project.
     *
     * @return The root archive
     */
    public ApplicationArchive getRootArchive() {
        return root;
    }

    /**
     * Returns a list of all application archives, excluding the root archive.
     */
    public List<ApplicationArchive> getArchives() {
        return applicationArchives;
    }

    /**
     * Returns a list of all application archives, including the root archive.
     */
    public List<ApplicationArchive> getAllArchives() {
        return allArchives;
    }

    /**
     * @deprecated use {@link #getArchives()}
     * @return A set of all application archives, excluding the root archive
     */
    @Deprecated(since = "3.37", forRemoval = true)
    public Collection<ApplicationArchive> getApplicationArchives() {
        return applicationArchives;
    }

    /**
     * @deprecated use {@link #getAllArchives()}
     * @return A set of all application archives, including the root archive
     */
    @Deprecated(since = "3.37", forRemoval = true)
    public Set<ApplicationArchive> getAllApplicationArchives() {
        return new HashSet<>(allArchives);
    }

    /**
     * Returns the archive that contains the given class name, or null if the class cannot be found.
     *
     * @param className The class name
     * @return The application archive
     */
    public ApplicationArchive containingArchive(String className) {
        return containingArchive(DotName.createSimple(className));
    }

    public ApplicationArchive containingArchive(DotName className) {
        if (root.getIndex().getClassByName(className) != null) {
            return root;
        }
        for (ApplicationArchive i : applicationArchives) {
            if (i.getIndex().getClassByName(className) != null) {
                return i;
            }
        }
        return null;
    }

}
