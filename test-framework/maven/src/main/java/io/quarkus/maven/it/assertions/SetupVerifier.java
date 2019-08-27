package io.quarkus.maven.it.assertions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.util.Optional;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import io.quarkus.maven.CreateProjectMojo;
import io.quarkus.maven.utilities.MojoUtils;

public class SetupVerifier {

    public static void assertThatJarExists(File archive) throws Exception {
        JarVerifier jarVerifier = new JarVerifier(archive);
        jarVerifier.assertThatJarIsCreated();
        jarVerifier.assertThatJarHasManifest();
    }

    public static void assertThatJarContainsFile(File archive, String file) throws Exception {
        JarVerifier jarVerifier = new JarVerifier(archive);
        jarVerifier.assertThatFileIsContained(file);
    }

    public static void assertThatJarDoesNotContainFile(File archive, String file) throws Exception {
        JarVerifier jarVerifier = new JarVerifier(archive);
        jarVerifier.assertThatFileIsNotContained(file);
    }

    public static void assertThatJarContainsFileWithContent(File archive, String path, String... lines) throws Exception {
        JarVerifier jarVerifier = new JarVerifier(archive);
        jarVerifier.assertThatFileContains(path, lines);
    }

    public static void verifySetup(File pomFile) throws Exception {
        assertNotNull(pomFile, "Unable to find pom.xml");
        MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
        Model model = xpp3Reader.read(new FileInputStream(pomFile));

        MavenProject project = new MavenProject(model);

        Optional<Plugin> maybe = MojoUtils.hasPlugin(project, CreateProjectMojo.PLUGIN_KEY);
        assertThat(maybe).isNotEmpty();

        //Check if the properties have been set correctly
        Properties properties = model.getProperties();
        assertThat(properties.containsKey("quarkus.version")).isTrue();

        // Check plugin is set
        Plugin plugin = maybe.orElseThrow(() -> new AssertionError("Plugin expected"));
        assertThat(plugin).isNotNull().satisfies(p -> {
            assertThat(p.getArtifactId()).isEqualTo(MojoUtils.getPluginArtifactId());
            assertThat(p.getGroupId()).isEqualTo(MojoUtils.getPluginGroupId());
            assertThat(p.getVersion()).isEqualTo(MojoUtils.QUARKUS_VERSION_PROPERTY);
        });

        // Check build execution Configuration
        assertThat(plugin.getExecutions()).hasSize(1).allSatisfy(execution -> {
            assertThat(execution.getGoals()).containsExactly("build");
            assertThat(execution.getConfiguration()).isNull();
        });

        // Check profile
        assertThat(model.getProfiles()).hasSize(1);
        Profile profile = model.getProfiles().get(0);
        assertThat(profile.getId()).isEqualTo("native");
        Plugin actual = profile.getBuild().getPluginsAsMap().get(CreateProjectMojo.PLUGIN_KEY);
        assertThat(actual).isNotNull();
        assertThat(actual.getExecutions()).hasSize(1).allSatisfy(exec -> {
            assertThat(exec.getGoals()).containsExactly("native-image");
            assertThat(exec.getConfiguration()).isInstanceOf(Xpp3Dom.class)
                    .satisfies(o -> assertThat(o.toString()).contains("enableHttpUrlHandler"));
        });
    }

    public static void verifySetupWithVersion(File pomFile) throws Exception {
        MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
        Model model = xpp3Reader.read(new FileInputStream(pomFile));

        MavenProject project = new MavenProject(model);
        Properties projectProps = project.getProperties();
        assertNotNull(projectProps);
        assertFalse(projectProps.isEmpty());
        assertEquals(MojoUtils.getPluginVersion(), projectProps.getProperty("quarkus.version"));
    }

}
