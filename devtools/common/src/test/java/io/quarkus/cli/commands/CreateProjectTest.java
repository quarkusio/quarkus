package io.quarkus.cli.commands;

import static io.quarkus.maven.utilities.MojoUtils.QUARKUS_VERSION_PROPERTY;
import static io.quarkus.maven.utilities.MojoUtils.getBomArtifactId;
import static io.quarkus.maven.utilities.MojoUtils.getPluginArtifactId;
import static io.quarkus.maven.utilities.MojoUtils.getPluginGroupId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.cli.commands.writer.FileWriter;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.templates.BuildTool;

public class CreateProjectTest {
    @Test
    public void create() throws IOException {
        final File file = new File("target/basic-rest");
        delete(file);
        final CreateProject createProject = new CreateProject(new FileWriter(file)).groupId("io.quarkus")
                .artifactId("basic-rest")
                .version("1.0.0-SNAPSHOT");

        Assertions.assertTrue(createProject.doCreateProject(new HashMap<>()));

        final File gitignore = new File(file, ".gitignore");
        Assertions.assertTrue(gitignore.exists());
        final String gitignoreContent = new String(Files.readAllBytes(gitignore.toPath()), StandardCharsets.UTF_8);
        Assertions.assertTrue(gitignoreContent.contains("\ntarget/\n"));
    }

    @Test
    public void createGradle() throws IOException {
        final File file = new File("target/basic-rest-gradle");
        delete(file);
        final CreateProject createProject = new CreateProject(new FileWriter(file)).groupId("io.quarkus")
                .artifactId("basic-rest")
                .version("1.0.0-SNAPSHOT")
                .buildTool(BuildTool.GRADLE);

        Assertions.assertTrue(createProject.doCreateProject(new HashMap<>()));

        final File gitignore = new File(file, ".gitignore");
        Assertions.assertTrue(gitignore.exists());
        final String gitignoreContent = new String(Files.readAllBytes(gitignore.toPath()), StandardCharsets.UTF_8);
        Assertions.assertFalse(gitignoreContent.contains("\ntarget/\n"));
        Assertions.assertTrue(gitignoreContent.contains("\nbuild/"));
        Assertions.assertTrue(gitignoreContent.contains("\n.gradle/\n"));
    }

    @Test
    public void createOnTopPomWithoutResource() throws IOException {
        final File testDir = new File("target/existing");
        delete(testDir);
        testDir.mkdirs();

        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId("org.acme");
        model.setArtifactId("foobar");
        model.setVersion("10.1.2");
        final File pom = new File(testDir, "pom.xml");
        MojoUtils.write(model, pom);
        final CreateProject createProject = new CreateProject(new FileWriter(testDir)).groupId("something.is")
                .artifactId("wrong")
                .version("1.0.0-SNAPSHOT");

        Assertions.assertTrue(createProject.doCreateProject(new HashMap<>()));

        assertThat(contentOf(pom, "UTF-8"))
                .contains(getPluginArtifactId(), QUARKUS_VERSION_PROPERTY, getPluginGroupId());
        assertThat(new File(testDir, "src/main/java")).isDirectory();
        assertThat(new File(testDir, "src/test/java")).isDirectory();

        assertThat(new File(testDir, "src/main/resources/application.properties")).exists();
        assertThat(new File(testDir, "src/main/resources/META-INF/resources/index.html")).isFile();
        assertThat(new File(testDir, "src/main/java")).isDirectory().matches(f -> {
            String[] list = f.list();
            return list != null && list.length == 0;
        });
        assertThat(new File(testDir, "src/test/java")).isDirectory().matches(f -> {
            String[] list = f.list();
            return list != null && list.length == 0;
        });

        assertThat(contentOf(new File(testDir, "pom.xml"), "UTF-8"))
                .containsIgnoringCase(getBomArtifactId());

    }

    @Test
    public void createOnTopPomWithResource() throws IOException {
        final File testDir = new File("target/existing");
        delete(testDir);
        testDir.mkdirs();

        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId("org.acme");
        model.setArtifactId("foobar");
        model.setVersion("10.1.2");
        final File pom = new File(testDir, "pom.xml");
        MojoUtils.write(model, pom);
        final CreateProject createProject = new CreateProject(new FileWriter(testDir)).groupId("something.is")
                .artifactId("wrong")
                .className("org.foo.MyResource")
                .version("1.0.0-SNAPSHOT");

        Assertions.assertTrue(createProject.doCreateProject(new HashMap<>()));

        assertThat(contentOf(pom, "UTF-8"))
                .contains(getPluginArtifactId(), QUARKUS_VERSION_PROPERTY, getPluginGroupId());
        assertThat(new File(testDir, "src/main/java")).isDirectory();
        assertThat(new File(testDir, "src/test/java")).isDirectory();

        assertThat(new File(testDir, "src/main/resources/application.properties")).exists();
        assertThat(new File(testDir, "src/main/resources/META-INF/resources/index.html")).exists();
        assertThat(new File(testDir, "src/main/java")).isDirectory();
        assertThat(new File(testDir, "src/main/java/org/foo/MyResource.java")).isFile();
        assertThat(new File(testDir, "src/test/java")).isDirectory();
        assertThat(new File(testDir, "src/test/java/org/foo/MyResourceTest.java")).isFile();
        assertThat(new File(testDir, "src/test/java/org/foo/NativeMyResourceIT.java")).isFile();

        assertThat(contentOf(new File(testDir, "pom.xml"))).contains(getBomArtifactId());

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

        Assertions.assertTrue(new CreateProject(new FileWriter(testDir)).groupId("org.acme")
                .artifactId("acme")
                .version("1.0.0-SNAPSHOT")
                .className("org.acme.MyResource")
                .doCreateProject(properties));

        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java/org/acme/MyResource.java")).isFile();
        assertThat(new File(testDir, "src/main/java/org/acme/MyApplication.java")).doesNotExist();

        assertThat(contentOf(pom, "UTF-8"))
                .contains(getPluginArtifactId(), QUARKUS_VERSION_PROPERTY, getPluginGroupId());
        assertThat(new File(testDir, "src/main/java")).isDirectory();
        assertThat(new File(testDir, "src/test/java")).isDirectory();

        assertThat(new File(testDir, "src/main/resources/application.properties")).exists();
        assertThat(new File(testDir, "src/main/resources/META-INF/resources/index.html")).exists();

        assertThat(contentOf(new File(testDir, "pom.xml"), "UTF-8"))
                .containsIgnoringCase(MojoUtils.QUARKUS_VERSION_PROPERTY);

    }

    public static void delete(final File file) throws IOException {

        if (file.exists()) {
            try (Stream<Path> stream = Files.walk(file.toPath())) {
                stream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }

        Assertions.assertFalse(
                Files.exists(file.toPath()), "Directory still exists");
    }
}
