package io.quarkus.cli.commands.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    private static GradleBuildFile buildFile;

    @BeforeAll
    public static void beforeAll() throws IOException {

        final File file = new File("target/test-classes/gradle-project");
        buildFile = new GradleBuildFile(new FileProjectWriter(file));
    }

    @Test
    void testGetDependencies() {
        List<Dependency> dependencies = buildFile.getDependencies();
        assertNotNull(dependencies);
        assertEquals(3, dependencies.size());
        List<String> depsString = new ArrayList<>();
        for (Iterator<Dependency> depIter = dependencies.iterator(); depIter.hasNext();) {
            Dependency dependency = depIter.next();
            String managementKey = dependency.getManagementKey();
            if (dependency.getVersion() != null && !dependency.getVersion().isEmpty()) {
                managementKey += ':' + dependency.getVersion();
            }
            depsString.add(managementKey);
        }
        assertArrayEquals(new Object[] { "io.quarkus:quarkus-jsonp:jar:0.20.0", "io.quarkus:quarkus-jsonb:jar:0.10.0",
                "io.quarkus:quarkus-resteasy:jar" }, depsString.toArray());
    }

    @Test
    void testGetProperty() {
        assertNull(buildFile.getProperty("toto"));
        assertEquals("999-SNAPSHOOT", buildFile.getProperty("quarkusVersion"));
    }

    @Test
    void testFindInstalled() {
        Map<String, Dependency> installed = buildFile.findInstalled();
        assertNotNull(installed);
        assertEquals(3, installed.size());
        Dependency jsonb = installed.get("io.quarkus:quarkus-jsonb");
        assertNotNull(jsonb);
        assertEquals("0.10.0", jsonb.getVersion());
        Dependency jsonp = installed.get("io.quarkus:quarkus-jsonp");
        assertNotNull(jsonp);
        assertEquals("0.20.0", jsonp.getVersion());
        Dependency resteasy = installed.get("io.quarkus:quarkus-resteasy");
        assertNotNull(resteasy);
        assertNull(resteasy.getVersion());
    }

}
