package io.quarkus.gradle;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;

import io.quarkus.bootstrap.model.AppArtifactCoords;

class GroovyBuildFileTest extends AbstractBuildFileTest {

    private static GroovyBuildFileFromConnector buildFile;

    @BeforeAll
    public static void beforeAll() throws URISyntaxException, IOException {
        buildFile = initializeProject("gradle-project", GroovyBuildFileFromConnector::new);
    }

    @Override
    String getProperty(String propertyName) throws IOException {
        return buildFile.getProperty(propertyName);
    }

    @Override
    List<AppArtifactCoords> getDependencies() throws IOException {
        return buildFile.getDependencies();
    }

}
