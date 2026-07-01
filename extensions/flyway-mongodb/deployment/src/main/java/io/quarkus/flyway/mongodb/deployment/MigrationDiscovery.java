package io.quarkus.flyway.mongodb.deployment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flywaydb.core.api.Location;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.runtime.util.ClassPathUtils;

/**
 * Discovers migration files from a list of Flyway locations, filtered by configured suffixes.
 * <p>
 * Logic is adapted from the JDBC Flyway processor
 * ({@code io.quarkus.flyway.deployment.FlywayProcessor}), with a configurable
 * file-suffix filter (default {@code .js}).
 */
final class MigrationDiscovery {

    private static final String CLASSPATH_APPLICATION_MIGRATIONS_PROTOCOL = "classpath";

    private static final Logger LOGGER = Logger.getLogger(MigrationDiscovery.class);

    private MigrationDiscovery() {
    }

    /**
     * Scans the given locations for migration files matching any of the given suffixes.
     *
     * @param locations the Flyway location strings
     * @param suffixes the file suffixes to include (e.g. {@code [".js", ".json"]})
     * @param hotDeploymentProducer producer to register hot-reload watched files/dirs
     * @param resourcesLocations output set that receives the classpath location paths
     * @return the set of resolved migration resource paths
     */
    static Set<String> findMigrations(List<String> locations,
            List<String> suffixes,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentProducer,
            Set<String> resourcesLocations) throws IOException {
        Set<String> applicationMigrationResources = new LinkedHashSet<>();
        discoverApplicationMigrations(locations, suffixes, hotDeploymentProducer, resourcesLocations,
                applicationMigrationResources);
        return applicationMigrationResources;
    }

    private static void discoverApplicationMigrations(List<String> locations,
            List<String> suffixes,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentProducer,
            Set<String> resourcesLocations,
            Set<String> applicationMigrationResources)
            throws IOException {
        for (String location : locations) {
            location = normalizeLocation(location);
            if (location.startsWith(Location.FILESYSTEM_PREFIX)) {
                applicationMigrationResources.add(location);
                continue;
            }

            resourcesLocations.add(location);
            String finalLocation = location;
            // We need to restart/reprocess this if new migration files are added
            hotDeploymentProducer.produce(HotDeploymentWatchedFileBuildItem.builder()
                    .setRestartNeeded(true)
                    .setLocationPredicate(l -> l.startsWith(finalLocation))
                    .build());
            ClassPathUtils.consumeAsPaths(Thread.currentThread().getContextClassLoader(), location, path -> {
                Set<String> applicationMigrations = null;
                try {
                    applicationMigrations = getApplicationMigrationsFromPath(finalLocation, path, suffixes);
                } catch (IOException e) {
                    LOGGER.warnv(e, "Can't process files in path %s", path);
                }
                if (applicationMigrations != null) {
                    applicationMigrationResources.addAll(applicationMigrations);
                }
            });
        }
    }

    private static String normalizeLocation(String location) {
        if (location == null) {
            throw new IllegalStateException("Flyway-MongoDB migration location may not be null.");
        }

        // Strip any 'classpath:' protocol prefixes because they are assumed
        // but not recognized by ClassLoader.getResources()
        if (location.startsWith(CLASSPATH_APPLICATION_MIGRATIONS_PROTOCOL + ':')) {
            location = location.substring(CLASSPATH_APPLICATION_MIGRATIONS_PROTOCOL.length() + 1);
            if (location.startsWith("/")) {
                location = location.substring(1);
            }
        }
        if (!location.endsWith("/")) {
            location += "/";
        }

        return location;
    }

    private static Set<String> getApplicationMigrationsFromPath(final String location, final Path rootPath,
            final List<String> suffixes)
            throws IOException {

        try (final Stream<Path> pathStream = Files.walk(rootPath)) {
            return pathStream.filter(Files::isRegularFile)
                    .filter(it -> {
                        // Compare against the file's name, not the full path — a path component
                        // that ends in ".js" should not make every file inside it look like a
                        // migration script.
                        String name = it.getFileName().toString();
                        return suffixes.stream().anyMatch(name::endsWith);
                    })
                    .map(it -> Paths.get(location, rootPath.relativize(it).toString()).normalize().toString())
                    // we don't want windows paths here since the paths are going to be used as classpath paths anyway
                    .map(it -> it.replace('\\', '/'))
                    .peek(it -> LOGGER.debugf("Discovered path: %s", it))
                    .collect(Collectors.toSet());
        }
    }
}
