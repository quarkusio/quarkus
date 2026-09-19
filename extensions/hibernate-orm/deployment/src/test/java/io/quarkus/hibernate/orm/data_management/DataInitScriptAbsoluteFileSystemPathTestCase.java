package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Test that setting {@code quarkus.hibernate-orm.data-management.init-script}
 * to the absolute path to a resource file on the filesystem
 * makes the build fail.
 *
 * The build used to run just fine because we were interpreting the "absolute" path
 * as relative to the FS root rather than relative to the classpath root,
 * and ended up deciding that it does exist... only to not be able to find it later and ignoring it.
 *
 * See https://github.com/quarkusio/quarkus/issues/23574
 */
public class DataInitScriptAbsoluteFileSystemPathTestCase {
    private static final String dataInitScriptAbsolutePath;
    private static final String escapedDataInitScriptAbsolutePath;
    static {
        // For this reproducer, we need the absolute path to a file
        // that actually exists in src/test/resources
        URL resource = DataInitScriptAbsoluteFileSystemPathTestCase.class.getResource("/import.sql");
        Path path;
        try {
            path = Paths.get(resource.toURI()).toAbsolutePath();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        dataInitScriptAbsolutePath = path.toString();
        System.out.println("Absolute filesystem path used in test: " + dataInitScriptAbsolutePath);
        if (path.getFileSystem().getSeparator().equals("\\")) {
            // "\" is a meta-character in property files, and thus it needs to be escaped for Windows paths.
            escapedDataInitScriptAbsolutePath = dataInitScriptAbsolutePath.replace("\\", "\\\\");
        } else {
            escapedDataInitScriptAbsolutePath = dataInitScriptAbsolutePath;
        }
        System.out.println(
                "Escaped absolute filesystem path passed to data-management.init-script: " + escapedDataInitScriptAbsolutePath);
    }

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.data-management.init-script", escapedDataInitScriptAbsolutePath)
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            "Unable to interpret path referenced in 'quarkus.hibernate-orm.data-management.init-script="
                                    + dataInitScriptAbsolutePath + "'",
                            "Expected a path relative to the root of the path tree"));

    @Test
    public void testDataInitScriptAbsolutePath() {
        // deployment exception should happen first
        Assertions.fail();
    }
}
