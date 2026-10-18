package io.quarkus.hibernate.orm.scripts;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusDevModeTest;

/**
 * Verifies that DDL scripts are generated when script generation is configured in application.properties.
 */
public class GenerateScriptTestCase {

    @RegisterExtension
    static QuarkusDevModeTest runner = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-generate-script.properties", "application.properties")
                    .addClasses(MyEntity.class));

    @BeforeAll
    public static void ensureScriptsDoNotExist() throws Exception {
        for (var script : List.of("/create.sql", "/drop.sql")) {
            var path = getScriptPath(script);
            if (path != null) {
                Files.deleteIfExists(path);
            }
        }
    }

    @Test
    public void verifyScriptExists() throws Exception {
        assertThat(getScriptPath("/create.sql")).content().contains("create table MyEntity");
        assertThat(getScriptPath("/drop.sql")).content().contains("drop table if exists MyEntity");
    }

    @RepeatedTest(2)
    public void verifyScriptIsOverwritten() throws Exception {
        assertThat(getScriptPath("/create.sql")).content().containsOnlyOnce("create table MyEntity");
    }

    private static Path getScriptPath(String path) throws URISyntaxException {
        var url = GenerateScriptTestCase.class.getResource(path);
        return url == null ? null : Path.of(url.toURI());
    }

}
