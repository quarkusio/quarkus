package io.quarkus.deployment.builditem;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.ApplicationArchive;

/**
 * A build item that holds information about the application's archives.
 * <p>
 * This includes the root application archive (usually the main artifact)
 * and any additional application archives (typically dependencies that
 * contain relevant metadata or classes for the application).
 * </p>
 * <p>
 * This class is immutable and used during build steps to provide access
 * to all application-level archives being processed.
 * </p>
 */
//temp class
public final class ApplicationArchivesBuildItem extends SimpleBuildItem {

    private final ApplicationArchive root;
    private final Collection<ApplicationArchive> applicationArchives;
    private final Set<ApplicationArchive> allArchives;

    /**
     * Creates a new {@code ApplicationArchivesBuildItem} instance.
     *
     * @param root the root application archive, typically the main deployment artifact.
     * @param applicationArchives the additional application archives (e.g., dependencies).
     *        Must not be {@code null}.
     */
    public ApplicationArchivesBuildItem(ApplicationArchive root, Collection<ApplicationArchive> applicationArchives) {
        this.root = root;
        this.applicationArchives = applicationArchives;

        HashSet<ApplicationArchive> ret = new HashSet<>(applicationArchives);
        ret.add(root);

        this.allArchives = Collections.unmodifiableSet(ret);
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
     * @return A set of all application archives, excluding the root archive
     */
    public Collection<ApplicationArchive> getApplicationArchives() {
        return applicationArchives;
    }

    /**
     * @return A set of all application archives, including the root archive
     */
    public Set<ApplicationArchive> getAllApplicationArchives() {
        return allArchives;
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

    /**
     * Returns the application archive that contains the specified class.
     *
     * <p>
     * This method searches for the class in the root archive first. If not found,
     * it then searches through the additional application archives.
     * </p>
     *
     * @param className the {@link DotName} representing the fully qualified name of the class to search for
     * @return the {@link ApplicationArchive} that contains the specified class, or {@code null} if not found in any archive
     */
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
