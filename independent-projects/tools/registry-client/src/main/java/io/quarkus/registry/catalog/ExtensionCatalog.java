package io.quarkus.registry.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ExtensionCatalog extends ExtensionOrigin {

    /**
     * All the origins this catalog is derived from.
     *
     * @return all the origins this catalog derives from.
     */
    List<ExtensionOrigin> getDerivedFrom();

    /**
     * Quarkus core version used by the extensions in this catalog.
     *
     * @return Quarkus core version used by the extensions in this catalog
     */
    String getQuarkusCoreVersion();

    /**
     * In case the catalog was built for a custom version of the Quarkus core,
     * this version represents the corresponding upstream community Quarkus core version.
     * This is done to be able to link the custom builds of Quarkus back to the upstream community
     * extensions ecosystem.
     *
     * This method may return null in case the corresponding version does not exist in the upstream community
     * or simply to link back to it.
     *
     *
     * @return the upstream community Quarkus core version corresponding to the Quarkus core version
     *         used in this catalog
     */
    String getUpstreamQuarkusCoreVersion();

    /**
     * Quarkus extensions that constitute the catalog.
     *
     * @return Quarkus extensions that constitute the catalog.
     */
    Collection<Extension> getExtensions();

    /**
     * Extension categories
     *
     * @return extension categories
     */
    List<Category> getCategories();

    /**
     * Various metadata attached to the catalog
     *
     * @return metadata attached to the catalog
     */
    Map<String, Object> getMetadata();
}
