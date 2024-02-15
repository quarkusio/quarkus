package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;
import io.quarkus.maven.dependency.ArtifactKey;

public class HibernateOrmRestDataCodestartIT {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .codestarts("hibernate-orm-rest-data")
            .extension(ArtifactKey.ga("io.quarkus", "quarkus-jdbc-h2"))
            .extension(ArtifactKey.ga("io.quarkus", "quarkus-resteasy-reactive-jackson"))
            .languages(JAVA)
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource(JAVA, "org.acme.MyEntity");
        codestartTest.checkGeneratedSource(JAVA, "org.acme.MyEntityResource");
    }

    @Test
    void testBuild() throws Throwable {
        codestartTest.buildAllProjects();
    }
}
