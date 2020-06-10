package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;

class GradleBuildFileTest {

    private static GradleBuildFileFromConnector buildFile;

    @BeforeAll
    public static void beforeAll() throws URISyntaxException {
        URL url = GradleBuildFileTest.class.getClassLoader().getResource("gradle-project");
        URI uri = url.toURI();
        Path gradleProjectPath = Paths.get(uri);
        final QuarkusPlatformDescriptor descriptor = Mockito.mock(QuarkusPlatformDescriptor.class);
        buildFile = new GradleBuildFileFromConnector(gradleProjectPath, descriptor);
    }

    @Test
    void testGetDependencies() throws IOException {
        List<Dependency> dependencies = buildFile.getDependencies();
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
        assertNull(buildFile.getProperty("toto"));
        assertEquals("0.23.2", buildFile.getProperty("quarkusVersion"));
    }

    @Test
    void testGetTestClosure() throws IOException {
        assertNull(buildFile.getProperty("test"));
    }

}
