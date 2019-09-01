package io.quarkus.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.cli.commands.writer.FileProjectWriter;

class GradleBuildFileTest {

    private static GradleBuildFileFromConnector buildFile;

    @BeforeAll
    public static void beforeAll() throws IOException, URISyntaxException {
        URL url = GradleBuildFileTest.class.getClassLoader().getResource("gradle-project");
        URI uri = url.toURI();
        Path gradleProjectPath;
        if (uri.getScheme().equals("jar")) {
            FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object> emptyMap());
            gradleProjectPath = fileSystem.getPath("gradle-project");
        } else {
            gradleProjectPath = Paths.get(uri);
        }
        Path gradleTestTmpDir = Files.createTempDirectory("gradle-test");
        try (Stream<Path> walk = Files.walk(gradleProjectPath, 1)) {
            for (Iterator<Path> it = walk.iterator(); it.hasNext();) {
                Path fileFromJar = it.next();
                Path tmpFile = Paths.get(gradleTestTmpDir.toString(), fileFromJar.getFileName().toString());
                Files.createFile(tmpFile);
                FileUtils.copyURLToFile(fileFromJar.toUri().toURL(), tmpFile.toFile());
            }
        }
        File tmpDirectoryFile = gradleTestTmpDir.toFile();
        tmpDirectoryFile.deleteOnExit();
        buildFile = new GradleBuildFileFromConnector(new FileProjectWriter(tmpDirectoryFile));
    }

    @Test
    void testGetDependencies() {
        List<Dependency> dependencies = buildFile.getDependencies();
        assertNotNull(dependencies);
        assertFalse(dependencies.isEmpty());
        List<String> depsString = new ArrayList<>();
        for (Iterator<Dependency> depIter = dependencies.iterator(); depIter.hasNext();) {
            Dependency dependency = depIter.next();
            String managementKey = dependency.getManagementKey();
            if (dependency.getVersion() != null && !dependency.getVersion().isEmpty()) {
                managementKey += ':' + dependency.getVersion();
            }
            depsString.add(managementKey);
        }
        assertTrue(depsString.contains("io.quarkus:quarkus-jsonp:jar:999-SNAPSHOT"));
        assertTrue(depsString.contains("io.quarkus:quarkus-jsonb:jar:999-SNAPSHOT"));
        assertTrue(depsString.contains("io.quarkus:quarkus-resteasy:jar:999-SNAPSHOT"));
    }

    @Test
    void testGetProperty() {
        assertNull(buildFile.getProperty("toto"));
        assertEquals("999-SNAPSHOT", buildFile.getProperty("quarkusVersion"));
    }

    @Test
    void testFindInstalled() throws IOException {
        Map<String, Dependency> installed = buildFile.findInstalled();
        assertNotNull(installed);
        assertFalse(installed.isEmpty());
        Dependency jsonb = installed.get("io.quarkus:quarkus-jsonb");
        assertNotNull(jsonb);
        assertEquals("999-SNAPSHOT", jsonb.getVersion());
        Dependency jsonp = installed.get("io.quarkus:quarkus-jsonp");
        assertNotNull(jsonp);
        assertEquals("999-SNAPSHOT", jsonp.getVersion());
        Dependency resteasy = installed.get("io.quarkus:quarkus-resteasy");
        assertNotNull(resteasy);
        assertEquals("999-SNAPSHOT", resteasy.getVersion());
    }

}
