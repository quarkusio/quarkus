package io.quarkus.gradle.application;

import java.io.IOException;
import java.nio.file.Path;

final class ContinuousTestingJavaParityProject extends ContinuousTestingParityProject {

    private static final String LOMBOK_VERSION = "1.18.42";

    ContinuousTestingJavaParityProject(Path projectDirectory) {
        super(projectDirectory);
    }

    void writeApplication() throws IOException {
        writeFile("settings.gradle", "rootProject.name = 'continuous-testing-java-parity'\n");
        writeFile("gradle.properties", "version = 999-SNAPSHOT\n");
        writeFile("build.gradle", """
                buildscript {
                    dependencies {
                        classpath files(%s)
                    }
                }

                apply plugin: 'io.quarkus.application'

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                dependencies {
                    implementation enforcedPlatform("io.quarkus:quarkus-bom:${project.property('version')}")
                    implementation "io.quarkus:quarkus-arc"
                    compileOnly "org.projectlombok:lombok:%s"
                    annotationProcessor "org.projectlombok:lombok:%s"
                    testCompileOnly "org.projectlombok:lombok:%s"
                    testAnnotationProcessor "org.projectlombok:lombok:%s"
                    testImplementation "io.quarkus:quarkus-junit:${project.property('version')}"
                }
                """.formatted(ContinuousBuildTestSupport.pluginClasspathFiles(),
                LOMBOK_VERSION, LOMBOK_VERSION, LOMBOK_VERSION, LOMBOK_VERSION));
        writeGreetingData("initial-source");
        writeFile("src/main/java/org/acme/GreetingService.java", """
                package org.acme;

                import jakarta.enterprise.context.ApplicationScoped;

                @ApplicationScoped
                public class GreetingService {
                    public String hello() {
                        return new GreetingData().getMessage();
                    }
                }
                """);
        writeFile("src/test/java/org/acme/ParityTest.java", """
                package org.acme;

                import static org.junit.jupiter.api.Assertions.assertNotNull;

                import java.io.IOException;
                import java.io.InputStream;
                import java.nio.charset.StandardCharsets;
                import java.nio.file.Files;
                import java.nio.file.Path;

                import jakarta.inject.Inject;

                import org.eclipse.microprofile.config.inject.ConfigProperty;
                import org.junit.jupiter.api.Test;

                import io.quarkus.test.junit.QuarkusTest;

                @QuarkusTest
                class ParityTest {
                    @Inject
                    GreetingService greetingService;

                    @ConfigProperty(name = "parity.message")
                    String mainResourceMessage;

                    @ConfigProperty(name = "parity.source-marker")
                    String sourceMarker;

                    @ConfigProperty(name = "parity.main-resource-marker")
                    String mainResourceMarker;

                    @ConfigProperty(name = "parity.test-resource-marker")
                    String testResourceMarker;

                    @Test
                    void recordsCurrentOutputs() throws IOException {
                        writeMarker(sourceMarker, greetingService.hello());
                        writeMarker(mainResourceMarker, mainResourceMessage);
                        try (InputStream stream = getClass().getResourceAsStream("/test-message.txt")) {
                            assertNotNull(stream);
                            writeMarker(testResourceMarker, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                        }
                    }

                    private static void writeMarker(String marker, String value) throws IOException {
                        Path path = Path.of(marker);
                        Files.createDirectories(path.getParent());
                        Files.writeString(path, value);
                    }
                }
                """);
        writeTestResource("initial-test-resource");
        writeApplicationProperties("initial-main-resource");
    }

    void writeGreetingData(String value) throws IOException {
        writeFile("src/main/java/org/acme/GreetingData.java", """
                package org.acme;

                import lombok.Value;

                @Value
                class GreetingData {
                    String message = "%s";
                }
                """.formatted(value));
    }

    void writeApplicationProperties(String value) throws IOException {
        writeFile("src/main/resources/application.properties", """
                parity.message=%s
                parity.source-marker=%s
                parity.main-resource-marker=%s
                parity.test-resource-marker=%s
                """.formatted(value,
                propertyPath("build/parity/source.txt"),
                propertyPath("build/parity/main-resource.txt"),
                propertyPath("build/parity/test-resource.txt")));
    }

    void writeTestResource(String value) throws IOException {
        writeFile("src/test/resources/test-message.txt", value + "\n");
    }
}
