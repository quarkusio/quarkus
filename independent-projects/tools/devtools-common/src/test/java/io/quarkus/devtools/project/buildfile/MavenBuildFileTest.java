package io.quarkus.devtools.project.buildfile;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
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
        QuarkusPlatformDescriptor mock = Mockito.mock(QuarkusPlatformDescriptor.class);
        mavenBuildFile = new MavenBuildFile(projectDirPath, mock);
    }

    @Test
    void shouldNotAddManagedDependencyWithProperties() throws IOException {
        AppArtifactCoords addedDep = new AppArtifactCoords("bar", "pops", "pom", "1");
        assertThat(mavenBuildFile.addDependency(addedDep, false)).isFalse();
    }

}
