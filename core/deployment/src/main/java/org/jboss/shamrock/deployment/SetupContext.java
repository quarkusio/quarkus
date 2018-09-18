package org.jboss.shamrock.deployment;

public interface SetupContext {

    /**
     * Adds a resource proessor
     *
     * @param resourceProcessor The resource processor
     */
    void addResourceProcessor(ResourceProcessor resourceProcessor);

    /**
     * Adds an injection provider
     *
     * @param injectionProvider The injection provider
     */
    void addInjectionProvider(InjectionProvider injectionProvider);

    /**
     * Adds an application archive marker file. If this file is present in the archive
     * then the archive is treated as an application archive, and will be indexed.
     *
     *
     * @param file The file location
     */
    void addApplicationArchiveMarker(String file);

    /**
     * Registers a capability name as being present, this can be queried via {@link ProcessorContext#isCapabilityPresent(String)}
     * @param name The capability name
     */
    void addCapability(String name);
}
