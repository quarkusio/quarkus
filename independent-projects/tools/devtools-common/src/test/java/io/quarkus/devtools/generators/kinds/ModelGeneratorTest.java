package io.quarkus.devtools.generators.kinds;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.generators.file.FileGenerator;
import io.quarkus.devtools.generators.kinds.model.ModelGenerator;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class ModelGeneratorTest {

    FileGenerator fileGenerator;
    QuarkusCommandInvocation quarkusCommandInvocation;
    QuarkusProject quarkusProject;
    ExtensionCatalog extensionCatalog;
    List<ResourceLoader> codestartResourceLoaders;
    ExtensionManager extensionManager;
    MessageWriter messageWriter;
    MavenProject mavenProject;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        extensionCatalog = mock(ExtensionCatalog.class);
        codestartResourceLoaders = new ArrayList<>();
        extensionManager = mock(ExtensionManager.class);
        messageWriter = mock(MessageWriter.class);
        mavenProject = mock(MavenProject.class);

        quarkusProject = QuarkusProject.of(
                Paths.get(tempDir.toString()),
                extensionCatalog, codestartResourceLoaders, messageWriter, extensionManager);
        quarkusCommandInvocation = new QuarkusCommandInvocation(quarkusProject);
        fileGenerator = new FileGenerator(quarkusCommandInvocation, mavenProject);
    }

    @Test
    public void shouldGenerateModel() throws IOException {
        Path output = Files.createDirectories(
                tempDir.resolve("src/main/java/io/quarkus/model"));

        Mockito.when(mavenProject.getGroupId()).thenReturn("io.quarkus");

        new ModelGenerator("model.mustache", quarkusCommandInvocation, mavenProject)
                .generate("User name:String age:Int created:Date value:Long");

        String expectedContent = "package io.quarkus.model;import io.quarkus.hibernate.orm.panache.PanacheEntity;import javax.persistence.Entity;import javax.persistence.Table;import java.time.LocalDate;@Entity@Table(name = \"user\")public class User extends PanacheEntity {    public String name;    public Integer age;    public LocalDate created;    public Long value;} ";
        Path expectedFile = Paths.get(output + "/User.java");

        assertAll(
                () -> assertTrue(Files.exists(expectedFile)),
                () -> assertEquals(expectedContent.trim(), String.join("", Files.readAllLines(expectedFile)).trim()));
    }

}
