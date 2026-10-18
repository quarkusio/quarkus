package io.quarkus.gradle;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Regression test for https://github.com/quarkusio/quarkus/issues/54142.
 *
 * htmlDependencyReport (from the project-report plugin) iterates the configurations
 * container, which re-fires whenObjectAdded listeners on temporary copies that Quarkus's
 * variant resolution creates, resolves, and removes. If any such listener mutates the
 * configuration (e.g. Kotlin's withDependencies), the build fails with
 * "Cannot mutate the state of configuration ... after the configuration was resolved".
 *
 * The bug reproduces both with and without the configuration cache, so we exercise both.
 */
public class HtmlDependencyReportTest extends QuarkusGradleWrapperTestBase {

    @ParameterizedTest(name = "configurationCache={0}")
    @ValueSource(booleans = { true, false })
    public void shouldRunHtmlDependencyReportWithoutMutationFailure(boolean configurationCache)
            throws IOException, URISyntaxException, InterruptedException {
        final File projectDir = getProjectDir("html-dependency-report");

        gradleConfigurationCache(configurationCache);

        runGradleWrapper(projectDir, "htmlDependencyReport");
    }
}
