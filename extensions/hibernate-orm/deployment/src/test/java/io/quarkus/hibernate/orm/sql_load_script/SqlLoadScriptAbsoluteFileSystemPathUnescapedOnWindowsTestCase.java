package io.quarkus.hibernate.orm.sql_load_script;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test that setting {@code quarkus.hibernate-orm.sql-load-script} to the absolute path to a resource file on the
 * filesystem on Windows, while also forgetting about the fact that backslashes need to be escaped in properties files,
 * makes the build fail. Added while working on https://github.com/quarkusio/quarkus/issues/23574, because we noticed
 * such paths cannot be correctly detected as being absolute (they cannot be distinguished from a weird relative path to
 * a file starting with "C:" and not containing any backslash).
 */
@EnabledOnOs(OS.WINDOWS)
public class SqlLoadScriptAbsoluteFileSystemPathUnescapedOnWindowsTestCase {
    private static final String sqlLoadScriptAbsolutePath;
    static {
        // For this reproducer, we need the absolute path to a file
        // that actually exists in src/test/resources
        URL resource = SqlLoadScriptAbsoluteFileSystemPathUnescapedOnWindowsTestCase.class.getResource("/import.sql");
        Path path;
        try {
            path = Paths.get(resource.toURI()).toAbsolutePath();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        System.out.println("Absolute filesystem path used in test: " + path);
        // This path will contain "\",
        // which is a meta-character and will be stripped by Quarkus when parsing the properties file,
        // resulting in the path being interpreted wrongly.
        // That's exactly what we want: we want to check that a user forgetting to escape backslashes
        // in a Windows path in a properties file will still get an error message,
        // even though it's not that clear.
        sqlLoadScriptAbsolutePath = path.toString();
        System.out.println(
                "(Unescaped) absolute filesystem path passed to sql-load-script: " + sqlLoadScriptAbsolutePath);
    }

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addClasses(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.sql-load-script", sqlLoadScriptAbsolutePath)
            .assertException(t -> assertThat(t).isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll("Unable to find file referenced in 'quarkus.hibernate-orm.sql-load-script="
                            // The path will appear without the backslashes in the error message;
                            // hopefully that'll be enough to hint at what went wrong.
                            + sqlLoadScriptAbsolutePath.replace("\\", "") + "'",
                            "Remove property or add file to your path"));

    @Test
    public void testSqlLoadScriptAbsolutePath() {
        // deployment exception should happen first
        Assertions.fail();
    }
}
