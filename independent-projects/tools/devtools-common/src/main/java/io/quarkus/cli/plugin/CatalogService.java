package io.quarkus.cli.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

public class CatalogService<T extends Catalog<T>> {

    protected static final Path USER_HOME = Paths.get(System.getProperty("user.home"));

    protected static final Predicate<Path> EXISTS_AND_WRITABLE = p -> p != null && p.toFile().exists() && p.toFile().canRead()
            && p.toFile().canWrite();
    protected static final Predicate<Path> IS_USER_HOME = p -> USER_HOME.equals(p);
    protected static final Predicate<Path> IS_ELIGIBLE_PROJECT_ROOT = EXISTS_AND_WRITABLE.and(Predicate.not(IS_USER_HOME));
    protected static final Predicate<Path> HAS_POM_XML = p -> p != null && p.resolve("pom.xml").toFile().exists();
    protected static final Predicate<Path> HAS_BUILD_GRADLE = p -> p != null && p.resolve("build.gradle").toFile().exists();

    protected static final Predicate<Path> GIT_ROOT = p -> p != null && p.resolve(".git").toFile().exists();

    protected final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(new Jdk8Module());

    protected final Class<T> catalogType;
    protected final Predicate<Path> projectRoot;
    protected final Function<Path, Path> relativePath;

    public CatalogService(Class<T> catalogType, Predicate<Path> projectRoot, Function<Path, Path> relativePath) {
        this.catalogType = catalogType;
        this.projectRoot = projectRoot;
        this.relativePath = relativePath;
    }

    /**
     * Reads the plguin catalog from the user home.
     *
     * @param userdir An optional path pointing to the user directory.
     * @return a catalog wrapped in optional or empty if the catalog is not present.
     */
    public Optional<T> readUserCatalog(Optional<Path> userDir) {
        Path userCatalogPath = getUserCatalogPath(userDir);
        return Optional.of(userCatalogPath).map(this::readCatalog);
    }

    public Optional<T> readProjectCatalog(Optional<Path> dir) {
        Optional<Path> projectCatalogPath = findProjectCatalogPath(dir);
        return projectCatalogPath.map(this::readCatalog);
    }

    /**
     * Get the project catalog path relative to the specified path.
     * The method will traverse from the specified path up to upmost directory that the user can write and
     * is under version control seeking for a `.quarkus/cli/plugins/catalog.json`.
     *
     * @param dir the specified path
     * @return the catalog path wrapped as {@link Optional} or empty if the catalog does not exist.
     */
    public Optional<Path> findProjectCatalogPath(Path dir) {
        return findProjectRoot(dir).map(relativePath);
    }

    public Optional<Path> findProjectCatalogPath(Optional<Path> dir) {
        return dir.flatMap(this::findProjectCatalogPath);
    }

    /**
     * Read the catalog from project or fallback to global catalog.
     *
     * @param projectDir An optional path pointing to the project directory.
     * @param userdir An optional path pointing to the user directory
     * @return the catalog
     */
    public Optional<T> readCatalog(Optional<Path> projectDir, Optional<Path> userDir) {
        return readProjectCatalog(projectDir).or(() -> readUserCatalog(userDir));
    }

    /**
     * Read the catalog from the specified path.
     *
     * @param path the path to read the catalog from.
     * @return the catalog
     */
    public T readCatalog(Path path) {
        try {
            return (path.toFile().length() == 0 ? catalogType.getConstructor().newInstance()
                    : objectMapper.readValue(path.toFile(), catalogType)).withCatalogLocation(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write the catalog to the specified {@link Path}.
     * The method will create the directory structure if missing.
     *
     * @param catalog the catalog
     * @param path the path
     */
    public void writeCatalog(T catalog) {
        try {
            File catalogFile = catalog.getCatalogLocation().map(Path::toFile)
                    .orElseThrow(() -> new IllegalStateException("Don't know where to save catalog!"));
            if (!catalogFile.exists() && !catalogFile.getParentFile().mkdirs() && !catalogFile.createNewFile()) {
                throw new IOException("Failed to create catalog at: " + catalogFile.getAbsolutePath());
            }
            objectMapper.writeValue(catalogFile, catalog.refreshLastUpdate());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the global catalog path that is under `.quarkus/cli/plugins/catalog.json` under the specified user home directory.
     * The specified directory is optional and the method will fallback to the `user.home` system property.
     * Using a different value if mostly needed for testing.
     *
     * @param userDir An optional user directory to use as a base path for the catalog lookup
     *
     * @return the catalog path wrapped as {@link Optional} or empty if the catalog does not exist.
     */
    public Path getUserCatalogPath(Optional<Path> userDir) {
        return relativePath.apply(userDir.orElse(USER_HOME));
    }

    /**
     * Get the global catalog path that is under `~/.quarkus/cli/plugins/catalog.json`
     *
     * @return the catalog path wrapped as {@link Optional} or empty if the catalog does not exist.
     */
    public Path getUserCatalogPath() {
        return getUserCatalogPath(Optional.empty());
    }

    /**
     * Get the catalog relative to the specified path.
     *
     * @param dir the specified path
     *
     * @return the catalog path wrapped as {@link Optional} or empty if the catalog does not exist.
     */
    public Optional<Path> getRelativeCatalogPath(Path dir) {
        return getRelativeCatalogPath(Optional.of(dir));
    }

    /**
     * Get the catalog relative to the current dir.
     *
     * @param output an {@link OutputOptionMixin} that can be used for tests to substitute current dir with a test directory.
     * @return the catalog path wrapped as {@link Optional} or empty if the catalog does not exist.
     */
    public Optional<Path> getRelativeCatalogPath(Optional<Path> dir) {
        return dir.or(() -> Optional.ofNullable(Paths.get(System.getProperty("user.dir")))).map(relativePath);
    }

    /**
     * Get the project or user catalog path.
     * The method with lookup the relative catalog path to the current dir and will fallback to the user catalog path.
     *
     * @param projectDir An optional path pointing to the project directory.
     * @param userdir An optional path pointing to the user directory
     * @return the catalog path wrapped as {@link Optional} or empty if the catalog does not exist.
     */
    public Optional<Path> getCatalogPath(Optional<Path> projectDir, Optional<Path> userDir) {
        return getRelativeCatalogPath(projectDir).filter(EXISTS_AND_WRITABLE)
                .or(() -> Optional.of(getUserCatalogPath(userDir)));
    }

    /**
     * Get the project root of the specified path.
     * The method will traverse from the specified path up to upmost directory that the user can write and
     * is under version control.
     *
     * @param dir the specified path
     * @return the project path wrapped as {@link Optional} or empty if the catalog does not exist.
     */
    public static Optional<Path> findProjectRoot(Path dir) {
        Optional<Path> lastKnownProjectDirectory = Optional.empty();
        for (Path current = dir; IS_ELIGIBLE_PROJECT_ROOT.test(current); current = current.getParent()) {
            if (GIT_ROOT.test(current)) {
                return Optional.of(current);
            }

            if (HAS_POM_XML.test(current)) {
                lastKnownProjectDirectory = Optional.of(current);
            }

            if (HAS_BUILD_GRADLE.test(current)) {
                lastKnownProjectDirectory = Optional.of(current);
            }
        }
        return lastKnownProjectDirectory;
    }

}
