package io.quarkus.flyway.runtime;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.api.resource.LoadableResource;
import org.flywaydb.core.internal.resource.classpath.ClassPathResource;
import org.flywaydb.core.internal.scanner.classpath.ResourceAndClassScanner;
import org.flywaydb.core.internal.scanner.filesystem.FileSystemScanner;
import org.jboss.logging.Logger;

/**
 * This class is used in order to prevent Flyway from doing classpath scanning which is both slow
 * and won't work in native mode
 */
@SuppressWarnings("rawtypes")
public final class QuarkusPathLocationScanner implements ResourceAndClassScanner {
    private static final Logger LOGGER = Logger.getLogger(QuarkusPathLocationScanner.class);
    private static final String LOCATION_SEPARATOR = "/";
    private static Collection<String> applicationMigrationFiles = Collections.emptyList(); // the set default to aid unit tests
    private static Collection<Class<? extends JavaMigration>> applicationMigrationClasses = Collections.emptyList(); // the set default to aid unit tests
    private static Map<String, Collection<Callback>> applicationCallbackClasses = Collections.emptyMap(); // the set default to aid unit tests

    private final Collection<LoadableResource> scannedResources;

    public QuarkusPathLocationScanner(Collection<Location> locations) {
        LOGGER.debugv("Locations: {0}", locations);

        this.scannedResources = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        FileSystemScanner fileSystemScanner = null;
        for (String migrationFile : applicationMigrationFiles) {
            if (isClassPathResource(locations, migrationFile)) {
                LOGGER.debugf("Loading %s", migrationFile);
                scannedResources.add(new ClassPathResource(null, migrationFile, classLoader, StandardCharsets.UTF_8));
            } else if (migrationFile.startsWith(Location.FILESYSTEM_PREFIX)) {
                if (fileSystemScanner == null) {
                    fileSystemScanner = new FileSystemScanner(StandardCharsets.UTF_8, false, false, false);
                }
                LOGGER.debugf("Checking %s for migration files", migrationFile);
                Collection<LoadableResource> resources = fileSystemScanner.scanForResources(new Location(migrationFile));
                LOGGER.debugf("%s contains %d migration files", migrationFile, resources.size());
                scannedResources.addAll(resources);
            }
        }

    }

    public static void setApplicationCallbackClasses(Map<String, Collection<Callback>> callbackClasses) {
        QuarkusPathLocationScanner.applicationCallbackClasses = callbackClasses;
    }

    public static Collection<Callback> callbacksForDataSource(String dsName) {
        return applicationCallbackClasses.getOrDefault(dsName, Collections.emptyList());
    }

    /**
     *
     * @return The resources that were found.
     */
    @Override
    public Collection<LoadableResource> scanForResources() {
        return scannedResources;
    }

    private boolean isClassPathResource(Collection<Location> locations, String migrationFile) {
        for (Location location : locations) {
            String locationPath = location.getPath();
            if (!locationPath.endsWith(LOCATION_SEPARATOR)) {
                locationPath += "/";
            }

            if (migrationFile.startsWith(locationPath)) {
                return true;
            } else {
                LOGGER.debugf("Migration file '%s' will be ignored because it does not start with '%s'", migrationFile,
                        locationPath);
            }
        }

        return false;
    }

    /**
     * Scans the classpath for concrete classes under the specified package implementing this interface.
     * Non-instantiable abstract classes are filtered out.
     *
     * @return The non-abstract classes that were found.
     */
    @Override
    public Collection<Class<? extends JavaMigration>> scanForClasses() {
        return applicationMigrationClasses;
    }

    public static void setApplicationMigrationFiles(Collection<String> applicationMigrationFiles) {
        QuarkusPathLocationScanner.applicationMigrationFiles = applicationMigrationFiles;
    }

    public static void setApplicationMigrationClasses(Collection<Class<? extends JavaMigration>> applicationMigrationClasses) {
        QuarkusPathLocationScanner.applicationMigrationClasses = applicationMigrationClasses;
    }
}
