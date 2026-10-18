package io.quarkus.cli.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.cli.CliDriver;

public class PluginCatalogServiceTest {

    private PluginCatalogService service = new PluginCatalogService();
    Path userRoot;
    Path projectRoot;

    @BeforeEach
    public void initial() throws Exception {
        userRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
                .resolve("target/test-classes/test-project/PluginCatalogServiceTest/user-root");
        CliDriver.deleteDir(userRoot);
        Files.createDirectories(userRoot);

        projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
                .resolve("target/test-classes/test-project/PluginCatalogServiceTest/project-root");
        CliDriver.deleteDir(projectRoot);
        Files.createDirectories(projectRoot);
        Files.createDirectories(projectRoot.resolve(".git"));
    }

    @Test
    public void shouldMapToCatalogFile() {
        Optional<Path> path = service.getRelativeCatalogPath(Optional.of(userRoot));
        assertTrue(path.isPresent());
        assertTrue(path.get().getFileName().endsWith("quarkus-cli-catalog.json"));
        assertTrue(path.get().toAbsolutePath().toString().contains("user-root"));

        path = service.getRelativeCatalogPath(Optional.of(projectRoot));
        assertTrue(path.isPresent());
        assertTrue(path.get().getFileName().endsWith("quarkus-cli-catalog.json"));
        assertTrue(path.get().toAbsolutePath().toString().contains("project-root"));
    }

    @Test
    public void shouldFallbackToUserRootCatalog() {
        Optional<Path> path = service.getCatalogPath(Optional.of(projectRoot), Optional.of(userRoot));
        assertTrue(path.isPresent());
        assertTrue(path.get().getFileName().endsWith("quarkus-cli-catalog.json"));
        assertTrue(path.get().toAbsolutePath().toString().contains("user-root"));
    }

    @Test
    public void shouldReadEmptyCatalog() {
        Optional<Path> catalogPath = service.getRelativeCatalogPath(Optional.of(userRoot));
        assertTrue(catalogPath.isPresent());

        PluginCatalog catalog = service.readCatalog(catalogPath.get());
        assertTrue(catalog.getPlugins().isEmpty());

        catalogPath = service.getRelativeCatalogPath(Optional.of(projectRoot));
        assertTrue(catalogPath.isPresent());
        catalog = service.readCatalog(catalogPath.get());
        assertTrue(catalog.getPlugins().isEmpty());
    }

    @Test
    public void shouldCreateCatalogInProjectRoot() {
        shouldCreateCatalogIn(service.findProjectCatalogPath(Optional.of(projectRoot)).orElseThrow(), "foo");
    }

    @Test
    public void shouldCreateCatalogInUserRoot() {
        shouldCreateCatalogIn(service.getRelativeCatalogPath(Optional.of(userRoot)).orElseThrow(), "foo");
    }

    @Test
    public void shouldCombineCatalogs() {
        Path userCatalog = service.getRelativeCatalogPath(Optional.of(userRoot)).orElseThrow();
        Path projectCatalog = service.findProjectCatalogPath(Optional.of(projectRoot)).orElseThrow();
        shouldCreateCatalogIn(userCatalog, "foo", "bar");
        shouldCreateCatalogIn(projectCatalog, "foo", "baz");
        PluginCatalog catalog = service.readCombinedCatalog(Optional.of(projectRoot), Optional.of(userRoot));
        assertTrue(catalog.getPlugins().size() == 3);
        assertTrue(catalog.getPlugins().containsKey("foo"));
        assertTrue(catalog.getPlugins().containsKey("bar"));
        assertTrue(catalog.getPlugins().containsKey("baz"));

        //The project catalog should alway override the user catalog, so `foo` that is used in both should be read from project
        assertEquals(catalog.getPlugins().get("foo").getCatalogLocation().map(Path::toAbsolutePath).map(Path::toString).get(),
                projectCatalog.toAbsolutePath().toString());
    }

    @Test
    public void shouldSyncWhenProjectFileIsNewerThanCatalog() throws IOException {
        PluginCatalog catalog = service.readCombinedCatalog(Optional.of(projectRoot), Optional.of(userRoot));
        Files.createFile(projectRoot.resolve("pom.xml"));
        assertTrue(PluginUtil.shouldSync(projectRoot, catalog));
    }

    @Test
    public void shouldNotSyncWhenProjectFileIsOlderThanCatalog() throws IOException {
        Files.createFile(projectRoot.resolve("pom.xml"));
        PluginCatalog catalog = new PluginCatalog("v1", LocalDateTime.now().plusMinutes(1), Collections.emptyMap(),
                Optional.empty());
        assertFalse(PluginUtil.shouldSync(projectRoot, catalog));
    }

    public void shouldCreateCatalogIn(Path catalogPath, String... commands) {
        File catalogFile = catalogPath.toFile();

        assertFalse(catalogFile.exists());

        Map<String, Plugin> plugins = new HashMap<>();

        //Let's populate a few commands
        for (String command : commands) {
            plugins.put(command,
                    new Plugin(command, PluginType.jbang, Optional.of(command), Optional.empty()));
            PluginCatalog catalog = new PluginCatalog(plugins).withCatalogLocation(catalogFile);
            service.writeCatalog(catalog);
        }

        //Let's read the catalog and verify it's content
        assertTrue(catalogFile.exists());
        PluginCatalog catalog = service.readCatalog(catalogPath);
        for (String command : commands) {
            assertTrue(catalog.getPlugins().containsKey(command));
        }
    }
}
