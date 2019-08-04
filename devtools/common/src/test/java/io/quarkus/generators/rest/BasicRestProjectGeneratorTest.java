package io.quarkus.generators.rest;

import static io.quarkus.generators.ProjectGenerator.*;
import static io.quarkus.maven.utilities.MojoUtils.getPluginVersion;
import static org.mockito.Mockito.*;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.google.common.collect.ImmutableMap;

import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.generators.BuildTool;
import io.quarkus.generators.SourceType;

class BasicRestProjectGeneratorTest {

    private static final Map<String, Object> BASIC_PROJECT_CONTEXT = ImmutableMap.<String, Object> builder()
            .put(PROJECT_GROUP_ID, "org.example")
            .put(PROJECT_ARTIFACT_ID, "quarkus-app")
            .put(PROJECT_VERSION, "0.0.1-SNAPSHOT")
            .put(QUARKUS_VERSION, getPluginVersion())
            .put(SOURCE_TYPE, SourceType.JAVA)
            .put(ADDITIONAL_GITIGNORE_ENTRIES, BuildTool.MAVEN.getGitIgnoreEntries())
            .put(PACKAGE_NAME, "org.example")
            .put(CLASS_NAME, "ExampleResource")
            .put("path", "/hello")
            .build();

    @Test
    @Timeout(1)
    @DisplayName("Should generate correctly multiple times in parallel with multiple threads")
    void generateMultipleTimes() throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(10);
        final CountDownLatch latch = new CountDownLatch(20);
        final BasicRestProjectGenerator basicRestProjectGenerator = new BasicRestProjectGenerator();
        List<Callable<Void>> collect = IntStream.range(0, 20).boxed().map(i -> (Callable<Void>) () -> {
            File file = Files.createTempDirectory("test").toFile();
            FileProjectWriter writer = new FileProjectWriter(file);
            basicRestProjectGenerator.generate(writer, BASIC_PROJECT_CONTEXT);
            latch.countDown();
            file.delete();
            return null;
        }).collect(Collectors.toList());
        executorService.invokeAll(collect);
        latch.await();
    }

    @Test
    @DisplayName("Should generate project files with basic context")
    void generateFiles() throws Exception {
        final ProjectWriter mockWriter = mock(ProjectWriter.class);
        final BasicRestProjectGenerator basicRestProjectGenerator = new BasicRestProjectGenerator();

        when(mockWriter.mkdirs(anyString())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0, String.class));

        basicRestProjectGenerator.generate(mockWriter, BASIC_PROJECT_CONTEXT);

        verify(mockWriter, times(9)).mkdirs(anyString());
        verify(mockWriter, times(2)).mkdirs("");
        verify(mockWriter, times(1)).mkdirs("src/main/java");
        verify(mockWriter, times(1)).mkdirs("src/main/java/org/example");
        verify(mockWriter, times(1)).mkdirs("src/test/java");
        verify(mockWriter, times(1)).mkdirs("src/test/java/org/example");
        verify(mockWriter, times(1)).mkdirs("src/main/resources");
        verify(mockWriter, times(1)).mkdirs("src/main/resources/META-INF/resources");
        verify(mockWriter, times(1)).mkdirs("src/main/docker");

        verify(mockWriter, times(10)).write(anyString(), anyString());
        verify(mockWriter, times(1)).write(eq("pom.xml"),
                argThat(argument -> argument.contains("<groupId>org.example</groupId>")
                        && argument.contains("<artifactId>quarkus-app</artifactId")
                        && argument.contains("<version>0.0.1-SNAPSHOT</version>")
                        && argument.contains("<quarkus.version>" + getPluginVersion() + "</quarkus.version>")));
        verify(mockWriter, times(1)).write(eq("src/main/java/org/example/ExampleResource.java"),
                argThat(argument -> argument.contains("@Path(\"/hello\")")));
        verify(mockWriter, times(1)).write(eq("src/test/java/org/example/ExampleResourceTest.java"), anyString());
        verify(mockWriter, times(1)).write(eq("src/test/java/org/example/NativeExampleResourceIT.java"), anyString());
        verify(mockWriter, times(1)).write(eq("src/main/resources/application.properties"), anyString());
        verify(mockWriter, times(1)).write(eq("src/main/resources/META-INF/resources/index.html"), anyString());
        verify(mockWriter, times(1)).write(eq("src/main/docker/Dockerfile.native"), anyString());
        verify(mockWriter, times(1)).write(eq("src/main/docker/Dockerfile.jvm"), anyString());
        verify(mockWriter, times(1)).write(eq(".dockerignore"), anyString());
    }

}
