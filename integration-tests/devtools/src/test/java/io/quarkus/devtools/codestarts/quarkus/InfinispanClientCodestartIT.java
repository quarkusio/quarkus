package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.JAVA_VERSION;
import static io.quarkus.devtools.testing.SnapshotTesting.checkContains;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;
import io.quarkus.maven.dependency.ArtifactKey;

public class InfinispanClientCodestartIT {

    @RegisterExtension
    public static QuarkusCodestartTest codestartMavenTest = QuarkusCodestartTest.builder()
            .extension(ArtifactKey.ga("io.quarkus", "quarkus-infinispan-client"))
            .putData(JAVA_VERSION, "25")
            .languages(JAVA)
            .build();

    @RegisterExtension
    public static QuarkusCodestartTest codestartGradleTest = QuarkusCodestartTest.builder()
            .extension(ArtifactKey.ga("io.quarkus", "quarkus-infinispan-client"))
            .putData(JAVA_VERSION, "25")
            .buildTool(BuildTool.GRADLE)
            .languages(JAVA)
            .build();

    @RegisterExtension
    public static QuarkusCodestartTest codestartGradleKotlinTest = QuarkusCodestartTest.builder()
            .extension(ArtifactKey.ga("io.quarkus", "quarkus-infinispan-client"))
            .putData(JAVA_VERSION, "25")
            .buildTool(BuildTool.GRADLE_KOTLIN_DSL)
            .languages(JAVA)
            .build();

    @Test
    void testMavenContent() throws Throwable {
        codestartMavenTest.assertThatGeneratedFile(JAVA, "pom.xml")
                .satisfies(checkContains("<maven.compiler.release>25</maven.compiler.release>"))
                .satisfies(checkContains("<artifactId>protostream-processor</artifactId>"))
                .satisfies(checkContains("<annotationProcessorPaths>"));
    }

    @Test
    void testGradleContent() throws Throwable {
        codestartGradleTest.assertThatGeneratedFile(JAVA, "build.gradle")
                .satisfies(checkContains("JavaVersion.VERSION_25"))
                .satisfies(checkContains("org.infinispan.protostream:protostream-processor"));
    }

    @Test
    void testGradleKotlinContent() throws Throwable {
        codestartGradleKotlinTest.assertThatGeneratedFile(JAVA, "build.gradle.kts")
                .satisfies(checkContains("JavaVersion.VERSION_25"))
                .satisfies(checkContains("org.infinispan.protostream:protostream-processor"));
    }

    @Test
    void testMavenBuild() throws Throwable {
        codestartMavenTest.buildAllProjects();
    }

    @Test
    void testGradleBuild() throws Throwable {
        codestartGradleTest.buildAllProjects();
    }

    @Test
    void testGradleKotlinBuild() throws Throwable {
        codestartGradleKotlinTest.buildAllProjects();
    }
}
