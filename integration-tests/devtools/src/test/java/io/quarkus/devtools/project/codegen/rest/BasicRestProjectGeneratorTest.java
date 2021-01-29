package io.quarkus.devtools.project.codegen.rest;

import static io.quarkus.devtools.project.codegen.ProjectGenerator.BOM_VERSION;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.CLASS_NAME;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.IS_SPRING;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.PACKAGE_NAME;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.PROJECT_ARTIFACT_ID;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.PROJECT_GROUP_ID;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.PROJECT_VERSION;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.SOURCE_TYPE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.devtools.PlatformAwareTestBase;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.codegen.SourceType;
import io.quarkus.devtools.project.codegen.writer.ProjectWriter;
import io.quarkus.maven.utilities.MojoUtils;

class BasicRestProjectGeneratorTest extends PlatformAwareTestBase {

    private final Map<String, Object> basicProjectContext = new HashMap<>();

    @BeforeEach
    void setUp() {
        basicProjectContext.put(PROJECT_GROUP_ID, "org.example");
        basicProjectContext.put(PROJECT_ARTIFACT_ID, "quarkus-app");
        basicProjectContext.put(PROJECT_VERSION, "0.0.1-SNAPSHOT");
        basicProjectContext.put(BOM_VERSION, getBomVersion());
        basicProjectContext.put(PACKAGE_NAME, "org.example");
        basicProjectContext.put(CLASS_NAME, "ExampleResource");
        basicProjectContext.put("path", "/hello");
        basicProjectContext.put(SOURCE_TYPE, SourceType.JAVA);
    }

    @Test
    @Timeout(5)
    @DisplayName("Should generate correctly multiple times in parallel with multiple threads")
    void generateMultipleTimes() throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(4);
        final CountDownLatch latch = new CountDownLatch(20);
        final BasicRestProjectGenerator basicRestProjectGenerator = new BasicRestProjectGenerator();
        List<Callable<Void>> collect = IntStream.range(0, 20).boxed().map(i -> (Callable<Void>) () -> {
            final Path path = Files.createTempDirectory("test");
            try {
                basicRestProjectGenerator.generate(createQuarkusCommandInvocation(path));
            } finally {
                IoUtils.recursiveDelete(path);
            }
            latch.countDown();
            return null;
        }).collect(Collectors.toList());
        executorService.invokeAll(collect);
        latch.await();
    }

    @Test
    @DisplayName("Should generate project files with basic context")
    void generateFilesWithJaxRsResource() throws Exception {
        final ProjectWriter mockWriter = mock(ProjectWriter.class);
        final Path mockProjectPath = mock(Path.class);
        final BasicRestProjectGenerator basicRestProjectGenerator = new BasicRestProjectGenerator();

        when(mockWriter.mkdirs(anyString())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0, String.class));

        basicRestProjectGenerator.generate(mockWriter,
                createQuarkusCommandInvocation(mockProjectPath));

        verify(mockWriter, times(10)).mkdirs(anyString());
        verify(mockWriter, times(3)).mkdirs("");
        verify(mockWriter, times(1)).mkdirs("src/main/java");
        verify(mockWriter, times(1)).mkdirs("src/main/java/org/example");
        verify(mockWriter, times(1)).mkdirs("src/test/java");
        verify(mockWriter, times(1)).mkdirs("src/test/java/org/example");
        verify(mockWriter, times(1)).mkdirs("src/main/resources");
        verify(mockWriter, times(1)).mkdirs("src/main/resources/META-INF/resources");
        verify(mockWriter, times(1)).mkdirs("src/main/docker");

        verify(mockWriter, times(12)).write(anyString(), anyString());
        verify(mockWriter, times(1)).write(eq("pom.xml"),
                argThat(argument -> argument.contains("<groupId>org.example</groupId>")
                        && argument.contains("<artifactId>quarkus-app</artifactId")
                        && argument.contains("<version>0.0.1-SNAPSHOT</version>")
                        && argument.contains(
                                "<" + MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLATFORM_VERSION_NAME + ">" + getQuarkusCoreVersion()
                                        + "</" + MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLATFORM_VERSION_NAME + ">")));
        verify(mockWriter, times(1)).write(eq("src/main/java/org/example/ExampleResource.java"),
                argThat(argument -> argument.contains("@Path(\"/hello\")")));
        verify(mockWriter, times(1)).write(eq("src/test/java/org/example/ExampleResourceTest.java"), anyString());
        verify(mockWriter, times(1)).write(eq("src/test/java/org/example/NativeExampleResourceIT.java"), anyString());
        verify(mockWriter, times(1)).write(eq("src/main/resources/application.properties"), anyString());
        verify(mockWriter, times(1)).write(eq("src/main/resources/META-INF/resources/index.html"), anyString());
        verify(mockWriter, times(1)).write(eq("src/main/docker/Dockerfile.native"), anyString());
        verify(mockWriter, times(1)).write(eq("src/main/docker/Dockerfile.jvm"), anyString());
        verify(mockWriter, times(1)).write(eq("src/main/docker/Dockerfile.legacy-jar"), anyString());
        verify(mockWriter, times(1)).write(eq(".dockerignore"), anyString());
    }

    @Test
    @DisplayName("Should generate project files with basic spring web context")
    void generateFilesWithSpringControllerResource() throws Exception {
        final ProjectWriter mockWriter = mock(ProjectWriter.class);
        final Path mockProjectPath = mock(Path.class);
        final BasicRestProjectGenerator basicRestProjectGenerator = new BasicRestProjectGenerator();

        when(mockWriter.mkdirs(anyString())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0, String.class));

        QuarkusCommandInvocation springContext = createQuarkusCommandInvocation(mockProjectPath);
        springContext.setValue(IS_SPRING, Boolean.TRUE);
        basicRestProjectGenerator.generate(mockWriter, springContext);

        verify(mockWriter, times(1)).write(eq("src/main/java/org/example/ExampleResource.java"),
                argThat(argument -> argument.contains("@RequestMapping(\"/hello\")")));
        verify(mockWriter, times(1)).write(eq("src/main/java/org/example/ExampleResource.java"),
                argThat(argument -> argument.contains("@RestController")));

    }

    private QuarkusCommandInvocation createQuarkusCommandInvocation(Path projectPath) {
        return new QuarkusCommandInvocation(QuarkusProject.maven(projectPath, getPlatformDescriptor()),
                basicProjectContext);
    }

}
