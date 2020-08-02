package io.quarkus.registry;

import io.quarkus.dependencies.Extension;
import io.quarkus.devtools.project.extensions.ExtensionInstallPlan;
import java.util.Collection;

public interface ExtensionRegistry {

    // Query methods
    Collection<String> getQuarkusCoreVersions();

    Collection<Extension> getExtensionsByCoreVersion(String version);

    /**
     * Return the set of extensions that matches a given keyword
     *
     * @param quarkusCore the quarkus core this extension supports
     * @param keyword the keyword to search
     * @return a set of {@link Extension} objects or an empty set if not found
     */
    Collection<Extension> list(String quarkusCore, String keyword);

    /**
     * What platforms and extensions do I need to add to my build descriptor?
     * This method looks up the registry and provides a {@link ExtensionInstallPlan} containing this information.
     *
     * @param quarkusCore the quarkus core this extension supports
     * @param keywords the keywords to lookup
     * @return a {@link ExtensionInstallPlan} an object representing the necessary data to be added to a build descriptor
     */
    ExtensionInstallPlan planInstallation(String quarkusCore, Collection<String> keywords);

}
