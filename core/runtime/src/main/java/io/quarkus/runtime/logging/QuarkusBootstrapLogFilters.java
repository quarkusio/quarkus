package io.quarkus.runtime.logging;

import java.util.List;

/**
 * Service provider interface for declaring log cleanup filters that must be active
 * <em>before</em> the Quarkus augmentation phase starts.
 *
 * <p>
 * Extensions that produce {@link io.quarkus.deployment.logging.LogCleanupFilterBuildItem}s to silence
 * known-noisy library log messages should also implement this interface and register it via the standard
 * {@link java.util.ServiceLoader} mechanism (i.e. a
 * {@code META-INF/services/io.quarkus.runtime.logging.QuarkusBootstrapLogFilters} file in the
 * <em>deployment</em> module). This ensures those same filters are applied during {@code @QuarkusTest}
 * runs, where augmentation-phase logs would otherwise bypass the recorder-configured cleanup filters.
 *
 * <p>
 * The service is loaded by {@code QuarkusTestExtension} using the augmentation classloader, so the
 * implementation class must be in the extension's deployment JAR.
 */
public interface QuarkusBootstrapLogFilters {

    /**
     * Returns the list of log cleanup filter elements that should be installed before augmentation.
     *
     * @return non-null list of filter elements; may be empty
     */
    List<LogCleanupFilterElement> getLogCleanupFilters();
}
