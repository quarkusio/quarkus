package io.quarkus.hibernate.orm.sql_load_script;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test that setting {@code quarkus.hibernate-orm.sql-load-script}
 * to the absolute path to a resource file on the filesystem
 * makes the build fail.
 *
 * The build used to run just fine because we were interpreting the "absolute" path
 * as relative to the FS root rather than relative to the classpath root,
 * and ended up deciding that it does exist... only to not be able to find it later and ignoring it.
 *
 * See https://github.com/quarkusio/quarkus/issues/23574
 */
public class AbsoluteFileSystemPathSqlLoadScriptTestCase {
    private static final String sqlLoadScriptAbsolutePath;
    static {
        // For this reproducer, we need the absolute path to a file
        // that actually exists in src/test/resources
        URL resource = AbsoluteFileSystemPathSqlLoadScriptTestCase.class.getResource("/import.sql");
        try {
            sqlLoadScriptAbsolutePath = Paths.get(resource.toURI()).toAbsolutePath().toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.sql-load-script", sqlLoadScriptAbsolutePath)
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll("Unable to find file referenced in '"
                            + "quarkus.hibernate-orm.sql-load-script=" + sqlLoadScriptAbsolutePath
                            + "'. Remove property or add file to your path."));

    @Test
    public void testSqlLoadScriptFileAbsentTest() {
        // deployment exception should happen first
        Assertions.fail();
    }
}
