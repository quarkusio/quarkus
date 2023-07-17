package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.KOTLIN;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;
import io.quarkus.maven.dependency.ArtifactKey;

public class HibernateOrmCodestartIT {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .codestarts("hibernate-orm")
            .extension(ArtifactKey.ga("io.quarkus", "quarkus-jdbc-h2"))
            .languages(JAVA, KOTLIN)
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource(JAVA, "org.acme.MyEntity");
        codestartTest.checkGeneratedSource(KOTLIN, "org.acme.MyKotlinEntity");
        codestartTest.assertThatGeneratedFileMatchSnapshot(JAVA, "src/main/resources/import.sql");
        codestartTest.assertThatGeneratedFileMatchSnapshot(KOTLIN, "src/main/resources/import.sql");
    }

    @Test
    void testBuild() throws Throwable {
        codestartTest.buildAllProjects();
    }
}
