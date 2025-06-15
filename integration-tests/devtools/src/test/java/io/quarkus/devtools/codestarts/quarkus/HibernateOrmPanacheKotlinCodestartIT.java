package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.KOTLIN;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;
import io.quarkus.maven.ArtifactKey;

public class HibernateOrmPanacheKotlinCodestartIT {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder().codestarts("hibernate-orm")
            .extension(new ArtifactKey("io.quarkus", "quarkus-jdbc-h2"))
            .extension(new ArtifactKey("io.quarkus", "quarkus-hibernate-orm-panache-kotlin")).languages(KOTLIN).build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.MyKotlinEntity");
        codestartTest.assertThatGeneratedFileMatchSnapshot(KOTLIN, "src/main/resources/import.sql");
    }

    @Test
    void testBuild() throws Throwable {
        codestartTest.buildAllProjects();
    }
}
