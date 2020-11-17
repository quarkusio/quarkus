package io.quarkus.devtools.codestarts.jbang;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.PlatformAwareTestBase;
import io.quarkus.devtools.ProjectTestUtil;

class QuarkusJBangCodestartGenerationTest extends PlatformAwareTestBase {

    private static final Path testDirPath = Paths.get("target/jbang-codestart-gen-test");

    @BeforeAll
    static void setUp() throws IOException {
        ProjectTestUtil.delete(testDirPath.toFile());
    }

    @Test
    void generateDefaultProject() throws IOException {
        final QuarkusJBangCodestartProjectInput input = QuarkusJBangCodestartProjectInput.builder()
                .putData("quarkus.version", "999-SNAPSHOT")
                .build();
        final Path projectDir = testDirPath.resolve("default");
        getCatalog().createProject(input).generate(projectDir);

        assertThat(projectDir.resolve("jbang")).exists();
        assertThat(projectDir.resolve("src/GreetingResource.java")).exists();

    }

    private QuarkusJBangCodestartCatalog getCatalog() throws IOException {
        return QuarkusJBangCodestartCatalog.fromQuarkusPlatformDescriptor(getPlatformDescriptor());
    }

}
