package io.quarkus.cli.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import io.quarkus.cli.commands.file.GradleBuildFile;
import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.cli.commands.writer.ZipProjectWriter;
import io.quarkus.maven.utilities.MojoUtils;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreateProjectTest extends PlatformAwareTestBase {
    @Test
    public void create() throws IOException {
        final File file = new File("target/basic-rest");
        delete(file);
        final CreateProject createProject = new CreateProject(new FileProjectWriter(file)).groupId("io.quarkus")
                .artifactId("basic-rest")
                .version("1.0.0-SNAPSHOT");

        assertTrue(createProject.doCreateProject(new HashMap<>()));

        final File gitignore = new File(file, ".gitignore");
        assertTrue(gitignore.exists());
        final String gitignoreContent = new String(Files.readAllBytes(gitignore.toPath()), StandardCharsets.UTF_8);
        assertTrue(gitignoreContent.contains("\ntarget/\n"));
    }

    @Test
    public void createGradle() throws IOException {
        final File file = new File("target/basic-rest-gradle");
        delete(file);
        FileProjectWriter writer = new FileProjectWriter(file);
        final CreateProject createProject = new CreateProject(writer).groupId("io.quarkus")
                .artifactId("basic-rest")
                .version("1.0.0-SNAPSHOT")
                .buildFile(new GradleBuildFile(writer));

        assertTrue(createProject.doCreateProject(new HashMap<>()));

        final File gitignore = new File(file, ".gitignore");
        assertTrue(gitignore.exists());
        final String gitignoreContent = new String(Files.readAllBytes(gitignore.toPath()), StandardCharsets.UTF_8);
        Assertions.assertFalse(gitignoreContent.contains("\ntarget/\n"));
        assertTrue(gitignoreContent.contains("\nbuild/"));
        assertTrue(gitignoreContent.contains("\n.gradle/\n"));

        assertThat(new File(file, "README.md")).exists();
        assertThat(contentOf(new File(file, "README.md"), "UTF-8")).contains("./gradlew");
    }

    @Test
    public void createGradleOnExisting() throws IOException {
        final File testDir = new File("target/existing");
        delete(testDir);
        testDir.mkdirs();

        final File buildGradle = new File(testDir, "build.gradle");
        buildGradle.createNewFile();
        final File settingsGradle = new File(testDir, "settings.gradle");
        settingsGradle.createNewFile();

        FileProjectWriter writer = new FileProjectWriter(testDir);
        final CreateProject createProject = new CreateProject(writer).groupId("io.quarkus")
                .artifactId("basic-rest")
                .version("1.0.0-SNAPSHOT")
                .buildFile(new GradleBuildFile(writer));

        assertTrue(createProject.doCreateProject(new HashMap<>()));

        final File gitignore = new File(testDir, ".gitignore");
        assertTrue(gitignore.exists());
        final String gitignoreContent = new String(Files.readAllBytes(gitignore.toPath()), StandardCharsets.UTF_8);
        Assertions.assertFalse(gitignoreContent.contains("\ntarget/\n"));
        assertTrue(gitignoreContent.contains("\nbuild/"));
        assertTrue(gitignoreContent.contains("\n.gradle/\n"));

        assertThat(contentOf(new File(testDir, "build.gradle"), "UTF-8"))
                .contains("id 'io.quarkus'")
                .contains("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}");

        final Properties props = new Properties();
        try (InputStream is = Files.newInputStream(testDir.toPath().resolve("gradle.properties"))) {
            props.load(is);
        }
        Assertions.assertEquals(getBomGroupId(), props.get("quarkusPlatformGroupId"));
        Assertions.assertEquals(getBomArtifactId(), props.get("quarkusPlatformArtifactId"));
        Assertions.assertEquals(getBomVersion(), props.get("quarkusPlatformVersion"));

        assertThat(new File(testDir, "README.md")).exists();
        assertThat(contentOf(new File(testDir, "README.md"), "UTF-8")).contains("./gradlew");
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
        final CreateProject createProject = new CreateProject(new FileProjectWriter(testDir)).groupId("something.is")
                .artifactId("wrong")
                .version("1.0.0-SNAPSHOT");

        assertTrue(createProject.doCreateProject(new HashMap<>()));

        assertThat(contentOf(pom, "UTF-8"))
                .contains(getPluginArtifactId(), MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLUGIN_VERSION_VALUE, getPluginGroupId());

        assertThat(new File(testDir, "src/main/java")).isDirectory();
        assertThat(new File(testDir, "src/test/java")).isDirectory();

        assertThat(new File(testDir, "README.md")).exists();
        assertThat(contentOf(new File(testDir, "README.md"), "UTF-8")).contains("./mvnw");
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
                .contains(MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLATFORM_ARTIFACT_ID_VALUE);

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
        final CreateProject createProject = new CreateProject(new FileProjectWriter(testDir)).groupId("something.is")
                .artifactId("wrong")
                .className("org.foo.MyResource")
                .version("1.0.0-SNAPSHOT");

        assertTrue(createProject.doCreateProject(new HashMap<>()));

        assertThat(contentOf(pom, "UTF-8"))
                .contains(getPluginArtifactId(), MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLUGIN_VERSION_VALUE, getPluginGroupId());

        assertThat(new File(testDir, "src/main/java")).isDirectory();
        assertThat(new File(testDir, "src/test/java")).isDirectory();

        assertThat(new File(testDir, "README.md")).exists();
        assertThat(contentOf(new File(testDir, "README.md"), "UTF-8")).contains("./mvnw");
        assertThat(new File(testDir, "src/main/resources/application.properties")).exists();
        assertThat(new File(testDir, "src/main/resources/META-INF/resources/index.html")).exists();
        assertThat(new File(testDir, "src/main/java")).isDirectory();
        assertThat(new File(testDir, "src/main/java/org/foo/MyResource.java")).isFile();
        assertThat(contentOf(new File(testDir, "src/main/java/org/foo/MyResource.java"), "UTF-8"))
                .contains("@Path", "@GET", "@Produces");
        assertThat(new File(testDir, "src/test/java")).isDirectory();
        assertThat(new File(testDir, "src/test/java/org/foo/MyResourceTest.java")).isFile();
        assertThat(new File(testDir, "src/test/java/org/foo/NativeMyResourceIT.java")).isFile();

        assertThat(contentOf(new File(testDir, "pom.xml"))).contains(getBomArtifactId());

    }

    @Test
    public void createOnTopPomWithSpringController() throws IOException {
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
        HashSet<String> extensions = new HashSet<>();
        extensions.add("spring-web");
        final CreateProject createProject = new CreateProject(new FileProjectWriter(testDir)).groupId("something.is")
                .artifactId("wrong")
                .className("org.foo.MyResource")
                .version("1.0.0-SNAPSHOT")
                .extensions(extensions);

        assertTrue(createProject.doCreateProject(new HashMap<>()));

        assertThat(contentOf(pom, "UTF-8"))
                .contains(getPluginArtifactId(), MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLUGIN_VERSION_VALUE, getPluginGroupId());

        assertThat(new File(testDir, "src/main/java")).isDirectory();
        assertThat(new File(testDir, "src/test/java")).isDirectory();

        assertThat(new File(testDir, "README.md")).exists();
        assertThat(contentOf(new File(testDir, "README.md"), "UTF-8")).contains("./mvnw");
        assertThat(new File(testDir, "src/main/resources/application.properties")).exists();
        assertThat(new File(testDir, "src/main/resources/META-INF/resources/index.html")).exists();
        assertThat(new File(testDir, "src/main/java")).isDirectory();
        assertThat(new File(testDir, "src/main/java/org/foo/MyResource.java")).isFile();
        assertThat(contentOf(new File(testDir, "src/main/java/org/foo/MyResource.java"), "UTF-8"))
                .contains("@RestController", "@RequestMapping", "@GetMapping");
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

        assertTrue(new CreateProject(new FileProjectWriter(testDir)).groupId("org.acme")
                                      .artifactId("acme")
                                      .version("1.0.0-SNAPSHOT")
                                      .className("org.acme.MyResource")
                                      .doCreateProject(properties));

        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java/org/acme/MyResource.java")).isFile();
        assertThat(new File(testDir, "src/main/java/org/acme/MyApplication.java")).doesNotExist();

        assertThat(contentOf(pom, "UTF-8"))
                .contains(getPluginArtifactId(), MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLUGIN_VERSION_VALUE, getPluginGroupId());

        assertThat(new File(testDir, "src/main/java")).isDirectory();
        assertThat(new File(testDir, "src/test/java")).isDirectory();

        assertThat(new File(testDir, "src/main/resources/application.properties")).exists();
        assertThat(new File(testDir, "src/main/resources/META-INF/resources/index.html")).exists();

        assertThat(contentOf(new File(testDir, "pom.xml"), "UTF-8"))
                .contains(MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLATFORM_VERSION_VALUE);

    }

    @Test
    @Timeout(2)
    @DisplayName("Should create correctly multiple times in parallel with multiple threads")
    void createMultipleTimes() throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(4);
        final CountDownLatch latch = new CountDownLatch(20);
        Map<String, Object> properties = new HashMap<>();
        properties.put("extensions", "commons-io:commons-io:2.5");

        List<Callable<Void>> collect = IntStream.range(0, 20).boxed().map(i -> (Callable<Void>) () -> {
            File tempDir = Files.createTempDirectory("test").toFile();
            FileProjectWriter write = new FileProjectWriter(tempDir);
            new CreateProject(write)
                    .groupId("org.acme")
                    .artifactId("acme")
                    .version("1.0.0-SNAPSHOT")
                    .className("org.acme.MyResource")
                    .doCreateProject(properties);
            latch.countDown();
            write.close();
            tempDir.delete();
            return null;
        }).collect(Collectors.toList());
        executorService.invokeAll(collect);
        latch.await();
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

    @Test
    public void createZip() throws IOException {
        final File file = new File("target/zip");
        delete(file);
        file.mkdirs();
        File zipFile = new File(file, "project.zip");
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos);
             ZipProjectWriter zipWriter = new ZipProjectWriter(zos)) {
            final CreateProject createProject = new CreateProject(zipWriter).groupId("io.quarkus")
                    .artifactId("basic-rest")
                    .version("1.0.0-SNAPSHOT");
            assertTrue(createProject.doCreateProject(new HashMap<>()));
        }
        assertTrue(zipFile.exists());
        File unzipProject = new File(file, "unzipProject");
        try (FileInputStream fis = new FileInputStream(zipFile); ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry zipEntry = zis.getNextEntry();
            byte[] buffer = new byte[1024];
            while (zipEntry != null) {
                File newFile = newFile(unzipProject, zipEntry);
                if (zipEntry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
        final File gitignore = new File(unzipProject, ".gitignore");
        assertTrue(gitignore.exists());
        final String gitignoreContent = new String(Files.readAllBytes(gitignore.toPath()), StandardCharsets.UTF_8);
        assertTrue(gitignoreContent.contains("\ntarget/\n"));
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
