package io.quarkus.gradle;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;

class KotlinBuildFileTest extends AbstractBuildFileTest {

    private static KotlinBuildFileFromConnector buildFile;

    @BeforeAll
    public static void beforeAll() throws URISyntaxException {
        URL url = KotlinBuildFileTest.class.getClassLoader().getResource("gradle-kts-project");
        URI uri = url.toURI();
        Path gradleProjectPath = Paths.get(uri);
        final QuarkusPlatformDescriptor descriptor = Mockito.mock(QuarkusPlatformDescriptor.class);
        buildFile = new KotlinBuildFileFromConnector(gradleProjectPath, descriptor);
    }

    @Override
    String getProperty(String propertyName) throws IOException {
        return buildFile.getProperty(propertyName);
    }

    @Override
    List<Dependency> getDependencies() throws IOException {
        return buildFile.getDependencies();
    }

}
