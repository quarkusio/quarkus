package org.jboss.shamrock.cli.commands;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.jboss.shamrock.maven.utilities.MojoUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateProjectTest {
    @Test
    public void create() throws IOException {
        final File file = new File("target/basic-rest");
        delete(file);
        final CreateProject createProject = new CreateProject(file).groupId("org.jboss.shamrock")
                                                                   .artifactId("basic-rest")
                                                                   .version("1.0.0-SNAPSHOT");

        Assert.assertTrue(createProject.doCreateProject(new HashMap<>()));
    }

    @Test
    public void createOnTopPom() throws IOException {
        final File testDir = new File("target/existing");
        delete(testDir);
        testDir.mkdirs();

        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId("com.acme");
        model.setArtifactId("foobar");
        model.setVersion("10.1.2");
        final File pom = new File(testDir, "pom.xml");
        MojoUtils.write(model, pom);
        final CreateProject createProject = new CreateProject(testDir).groupId("something.is")
                                                                   .artifactId("wrong")
                                                                   .version("1.0.0-SNAPSHOT");

        Assert.assertTrue(createProject.doCreateProject(new HashMap<>()));

        assertThat(FileUtils.readFileToString(pom, "UTF-8"))
            .contains(MojoUtils.SHAMROCK_PLUGIN_ARTIFACT_ID, MojoUtils.SHAMROCK_VERSION_VARIABLE, MojoUtils.SHAMROCK_GROUP_ID);
        assertThat(new File(testDir, "src/main/java")).isDirectory();
        assertThat(new File(testDir, "src/test/java")).isDirectory();

        assertThat(new File(testDir, "src/main/resources/META-INF/microprofile-config.properties")).exists();
        assertThat(new File(testDir, "src/main/resources/META-INF/resources/index.html")).exists();

        assertThat(FileUtils.readFileToString(new File(testDir, "pom.xml"), "UTF-8"))
            .containsIgnoringCase(MojoUtils.SHAMROCK_BOM_ARTIFACT_ID);

    }
    @Test
    public void createNewWithCustomizations() throws IOException {
        final File testDir = new File("target/existing");
        delete(testDir);
        testDir.mkdirs();

        final File pom = new File(testDir, "pom.xml");
        Map<String, Object> properties = new HashMap<>();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("className", "org.acme.MyResource");
        properties.put("extensions", "commons-io:commons-io:2.5");

        Assert.assertTrue(new CreateProject(testDir).groupId("org.acme")
                                                    .artifactId("acme")
                                                    .version("1.0.0-SNAPSHOT")
                                                    .doCreateProject(properties));

        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java/org/acme/MyResource.java")).isFile();
        assertThat(new File(testDir, "src/main/java/org/acme/MyApplication.java")).isFile();

        assertThat(FileUtils.readFileToString(pom, "UTF-8"))
            .contains(MojoUtils.SHAMROCK_PLUGIN_ARTIFACT_ID, MojoUtils.SHAMROCK_VERSION_VARIABLE, MojoUtils.SHAMROCK_GROUP_ID);
        assertThat(new File(testDir, "src/main/java")).isDirectory();
        assertThat(new File(testDir, "src/test/java")).isDirectory();

        assertThat(new File(testDir, "src/main/resources/META-INF/microprofile-config.properties")).exists();
        assertThat(new File(testDir, "src/main/resources/META-INF/resources/index.html")).exists();

        assertThat(FileUtils.readFileToString(new File(testDir, "pom.xml"), "UTF-8"))
            .containsIgnoringCase(MojoUtils.SHAMROCK_BOM_ARTIFACT_ID);

    }

    public static void delete(final File file) throws IOException {

        if (file.exists()) {
            Files.walk(file.toPath())
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }

        Assert.assertFalse("Directory still exists",
            Files.exists(file.toPath()));
    }
}