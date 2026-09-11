package io.quarkus.flyway.mongodb.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.resource.LoadableResource;
import org.flywaydb.core.internal.parser.ParsingContext;
import org.flywaydb.core.internal.resource.filesystem.FileSystemResource;
import org.flywaydb.core.internal.sqlscript.SqlScriptMetadata;
import org.flywaydb.core.internal.util.Pair;
import org.flywaydb.core.internal.util.UrlUtils;
import org.jboss.logging.Logger;

/**
 * Holds the migration files discovered at build time, keyed by MongoDB client name.
 * Populated via {@link FlywayMongodbRecorder#setApplicationMigrationFiles}.
 * <p>
 * Also exposes {@link #scanClasspath} as the replacement for Flyway's
 * {@code ClasspathSqlMigrationScanner.scan(...)}, installed by a build-time bytecode
 * transform that applies to both JVM and native builds. Required because Flyway's upstream
 * scanner dereferences {@code Thread.currentThread().getContextClassLoader().getResource(".")},
 * which returns {@code null} under the Quarkus runtime classloader.
 */
public final class QuarkusMongodbPathLocationScanner {

    private static final Logger LOGGER = Logger.getLogger(QuarkusMongodbPathLocationScanner.class);

    private static volatile Map<String, Collection<String>> applicationMigrationFiles = Collections.emptyMap();

    private QuarkusMongodbPathLocationScanner() {
    }

    public static void setApplicationMigrationFiles(Map<String, Collection<String>> filesByClient) {
        LOGGER.debugv("Setting application migration files: {0}", filesByClient);
        applicationMigrationFiles = Map.copyOf(filesByClient);
    }

    /**
     * All migration files from all clients, deduplicated.
     */
    public static Collection<String> allMigrationFiles() {
        return applicationMigrationFiles.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    /**
     * Quarkus-aware replacement for Flyway's
     * {@code org.flywaydb.scanners.ClasspathSqlMigrationScanner#scan(Location, Configuration, ParsingContext)}.
     * Returns resources backed by the build-time discovered migration files, using the
     * configuration's {@link ClassLoader} to materialize each migration as a {@link FileSystemResource}
     * whose {@code getAbsolutePath()} is a real on-disk path — required because Flyway's
     * {@code NonJdbcReader} reads migrations via {@code Files.newBufferedReader(Path.of(absolutePath))}.
     * Resources that live inside a jar (or have no {@code file:} URL) are extracted to a temp file.
     */
    public static Collection<Pair<LoadableResource, SqlScriptMetadata>> scanClasspath(Location location,
            Configuration configuration, ParsingContext parsingContext) {
        if (!"classpath:".equals(location.getPrefix())) {
            return List.of();
        }

        String rootPath = location.getPath();
        if (!rootPath.endsWith("/")) {
            rootPath = rootPath + "/";
        }

        Collection<String> allFiles = allMigrationFiles();
        List<Pair<LoadableResource, SqlScriptMetadata>> result = new ArrayList<>();
        ClassLoader classLoader = configuration.getClassLoader();

        for (String file : allFiles) {
            if (!file.startsWith(rootPath)) {
                continue;
            }
            String diskPath = resolveToDiskPath(file, classLoader);
            FileSystemResource resource = new FileSystemResource(location, diskPath, StandardCharsets.UTF_8, false);
            result.add(Pair.of(resource, null));
        }

        return result;
    }

    private static String resolveToDiskPath(String classpathResource, ClassLoader classLoader) {
        URL url = classLoader.getResource(classpathResource);
        if (url == null) {
            throw new FlywayException("Unable to locate migration resource on classpath: " + classpathResource);
        }
        if ("file".equals(url.getProtocol())) {
            return new java.io.File(UrlUtils.decodeURL(url.getPath())).getAbsolutePath();
        }
        return extractToTempFile(classpathResource, classLoader);
    }

    private static volatile Path extractionDir;

    private static String extractToTempFile(String classpathResource, ClassLoader classLoader) {
        String fileName = classpathResource;
        int lastSlash = fileName.lastIndexOf('/');
        if (lastSlash >= 0) {
            fileName = fileName.substring(lastSlash + 1);
        }
        try (InputStream in = classLoader.getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new FlywayException("Unable to open migration resource stream: " + classpathResource);
            }
            Path target = getOrCreateExtractionDir().resolve(fileName);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            target.toFile().deleteOnExit();
            return target.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new FlywayException("Unable to extract migration resource to temp file: " + classpathResource, e);
        }
    }

    /**
     * Lazily create one temp directory for all extracted migrations. Each migration is written
     * into it under its original filename so Flyway's {@code ResourceNameParser} can still
     * recognise the {@code V<version>__<description>.<ext>} pattern.
     */
    private static Path getOrCreateExtractionDir() throws IOException {
        Path dir = extractionDir;
        if (dir != null) {
            return dir;
        }
        synchronized (QuarkusMongodbPathLocationScanner.class) {
            if (extractionDir == null) {
                extractionDir = Files.createTempDirectory("quarkus-flyway-mongodb-");
                extractionDir.toFile().deleteOnExit();
            }
            return extractionDir;
        }
    }
}
