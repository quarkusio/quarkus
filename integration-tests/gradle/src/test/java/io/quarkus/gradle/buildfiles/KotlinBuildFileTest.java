package io.quarkus.gradle.buildfiles;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.gradle.KotlinBuildFileFromConnector;

@Disabled
class KotlinBuildFileTest extends AbstractBuildFileTest {

    private static KotlinBuildFileFromConnector buildFile;

    @BeforeAll
    public static void beforeAll() throws URISyntaxException, IOException {
        buildFile = initializeProject("buildfiles/gradle-kts-project", KotlinBuildFileFromConnector::new);
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
