package io.quarkus.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.cli.commands.writer.FileProjectWriter;

class GradleBuildFileTest {

    private static GradleBuildFileFromConnector buildFile;

    @BeforeAll
    public static void beforeAll() throws IOException {

        final File file = new File("build/resources/test/gradle-project");
        buildFile = new GradleBuildFileFromConnector(new FileProjectWriter(file));
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
