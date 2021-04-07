package io.quarkus.devtools.project.buildfile;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class MavenBuildFileTest {

    MavenBuildFile mavenBuildFile;

    @BeforeEach
    void setUp(@TempDir Path projectDirPath) throws IOException {
        Model model = new Model();
        Properties props = new Properties();
        props.setProperty("foo", "bar");
        props.setProperty("cheese", "pops");
        props.setProperty("one", "1");
        model.setProperties(props);
        DependencyManagement depMan = new DependencyManagement();
        Dependency dependency = new Dependency();
        dependency.setGroupId("${foo}");
        dependency.setArtifactId("${cheese}");
        dependency.setVersion("${one}");
        dependency.setType("pom");
        dependency.setScope("import");
        depMan.addDependency(dependency);
        model.setDependencyManagement(depMan);
        Path pomPath = projectDirPath.resolve("pom.xml");
        MojoUtils.write(model, pomPath.toFile());
        ExtensionCatalog mock = Mockito.mock(ExtensionCatalog.class);
        mavenBuildFile = new MavenBuildFile(projectDirPath, mock);
    }

    @Test
    void shouldNotAddManagedDependencyWithProperties() throws IOException {
        ArtifactCoords addedDep = new ArtifactCoords("bar", "pops", "pom", "1");
        assertThat(mavenBuildFile.addDependency(addedDep, false)).isFalse();
    }

}
