package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.Test;

abstract class AbstractBuildFileTest {

    abstract String getProperty(String propertyName) throws IOException;

    abstract List<Dependency> getDependencies() throws IOException;

    @Test
    void testGetDependencies() throws IOException {
        List<Dependency> dependencies = getDependencies();
        assertThat(dependencies).isNotEmpty();
        List<String> depsString = new ArrayList<>();
        for (Iterator<Dependency> depIter = dependencies.iterator(); depIter.hasNext();) {
            Dependency dependency = depIter.next();
            String managementKey = dependency.getManagementKey();
            if (dependency.getVersion() != null && !dependency.getVersion().isEmpty()) {
                managementKey += ':' + dependency.getVersion();
            }
            depsString.add(managementKey);
        }
        assertThat(depsString).contains("io.quarkus:quarkus-jsonp:jar:0.23.2",
                "io.quarkus:quarkus-jsonb:jar:0.23.2", "io.quarkus:quarkus-resteasy:jar:0.23.2");
    }

    @Test
    void testGetProperty() throws IOException {
        assertNull(getProperty("toto"));
        assertEquals("0.23.2", getProperty("quarkusVersion"));
    }

    @Test
    void testGetTestClosure() throws IOException {
        assertNull(getProperty("test"));
    }

}
