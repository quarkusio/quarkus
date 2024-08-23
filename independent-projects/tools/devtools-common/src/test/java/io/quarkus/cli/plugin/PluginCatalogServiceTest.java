package io.quarkus.cli.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

public class PluginCatalogServiceTest {

    PluginCatalogService service = new PluginCatalogService();

    Path rootDir;

    @BeforeEach
    public void setUp() throws Exception {
        rootDir = Files.createTempDirectory("quarkus-cli-test-project-root");
    }

    @AfterEach
    public void cleanUp() throws Exception {
        makeWritable(rootDir);
    }

    @Test
    public void shouldFindGitRootCatalogPath() throws Exception {
        Path expectedCatalogPath = PluginCatalogService.RELATIVE_CATALOG_JSON.apply(rootDir);
        Path moduleA = rootDir.resolve("module-a");
        Path moduleAA = moduleA.resolve("module-aa");
        Path dotGit = rootDir.resolve(".git");
        dotGit.toFile().mkdir();
        moduleAA.toFile().mkdirs();

        Optional<Path> result = service.findProjectCatalogPath(rootDir);
        assertEquals(expectedCatalogPath, result.get());

        result = service.findProjectCatalogPath(moduleA);
        assertEquals(expectedCatalogPath, result.get());
    }

    @Test
    public void shouldFindDotQuakursRootCatalogPath() throws Exception {
        Path moduleB = rootDir.resolve("module-b");
        Path moduleBA = moduleB.resolve("module-ba");

        Path dotGit = rootDir.resolve(".git");
        dotGit.toFile().mkdir();

        Path dotQuarkus = PluginCatalogService.RELATIVE_CATALOG_JSON.apply(moduleB);
        dotQuarkus.getParent().toFile().mkdirs();
        Files.write(dotQuarkus, new byte[0]);

        moduleBA.toFile().mkdirs();

        Path expectedCatalogPath = PluginCatalogService.RELATIVE_CATALOG_JSON.apply(rootDir);

        Optional<Path> result = service.findProjectCatalogPath(moduleB);
        assertEquals(expectedCatalogPath, result.get());

        result = service.findProjectCatalogPath(moduleBA);
        assertEquals(expectedCatalogPath, result.get());
    }

    @Test
    @DisabledOnOs(OS.WINDOWS) //Test changes File permissions
    public void shouldFindLastReadableCatalogPath() throws Exception {

        Path moduleC = rootDir.resolve("module-c");
        Path moduleCA = moduleC.resolve("module-ca");

        Path dotGit = rootDir.resolve(".git");
        dotGit.toFile().mkdir();

        moduleCA.toFile().mkdirs();
        // Parent not readable
        try {
            if (moduleC.toFile().setWritable(false)) {
                Optional<Path> result = service.findProjectCatalogPath(moduleCA);
                assertTrue(result.isEmpty());
            }
        } finally {
            moduleC.toFile().setWritable(true);
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS) //Test changes File permissions
    public void shouldFindLastMavenRootCatalogPath() throws Exception {

        Path moduleM = rootDir.resolve("module-m");
        Path moduleMA = moduleM.resolve("module-ma");
        Path moduleMAA = moduleMA.resolve("module-maa");

        Path dotGit = rootDir.resolve(".git");
        dotGit.toFile().mkdir();

        moduleMAA.toFile().mkdirs();

        Path pomMA = moduleMA.resolve("pom.xml");
        Files.write(pomMA, new byte[0]);

        Path pomMAA = moduleMAA.resolve("pom.xml");
        Files.write(pomMAA, new byte[0]);

        Path expectedCatalogPath = PluginCatalogService.RELATIVE_CATALOG_JSON.apply(moduleMA);

        // Parent not readable
        try {
            if (rootDir.toFile().setWritable(false)) {
                Optional<Path> result = service.findProjectCatalogPath(moduleMA);
                assertEquals(expectedCatalogPath, result.get());
            }
        } finally {
            moduleM.toFile().setWritable(true);
        }
        Optional<Path> result = service.findProjectCatalogPath(moduleMAA);
        assertEquals(expectedCatalogPath, result.get());
    }

    @Test
    @DisabledOnOs(OS.WINDOWS) //Test changes File permissions
    public void shouldFindLastGradleRootCatalogPath() throws Exception {

        Path moduleG = rootDir.resolve("module-g");
        Path moduleGA = moduleG.resolve("module-ga");
        Path moduleGAA = moduleGA.resolve("module-gaa");

        Path dotGit = rootDir.resolve(".git");
        dotGit.toFile().mkdir();

        moduleGAA.toFile().mkdirs();

        Path pomGA = moduleGA.resolve("build.gradle");
        Files.write(pomGA, new byte[0]);

        Path pomGAA = moduleGAA.resolve("build.gradle");
        Files.write(pomGAA, new byte[0]);

        Path expectedCatalogPath = PluginCatalogService.RELATIVE_CATALOG_JSON.apply(moduleGA);

        // Parent not readable
        try {
            if (rootDir.toFile().setWritable(false)) {
                Optional<Path> result = service.findProjectCatalogPath(moduleGA);
                assertEquals(expectedCatalogPath, result.get());
            }
        } finally {
            moduleG.toFile().setWritable(true);
        }
        Optional<Path> result = service.findProjectCatalogPath(moduleGAA);
        assertEquals(expectedCatalogPath, result.get());
    }

    private static void makeWritable(Path path) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path sub : stream) {
                if (Files.isDirectory(sub)) {
                    makeWritable(sub);
                }
            }
            path.toFile().setWritable(true);
        }
    }
}
