package io.quarkus.it.extension.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import io.quarkus.maven.it.RunAndCheckMojoTestBase;
import io.quarkus.maven.it.continuoustesting.ContinuousTestingMavenTestUtils;
import io.quarkus.test.config.QuarkusClassOrderer;

@DisabledIfSystemProperty(named = "quarkus.test.native", matches = "true")
public class ParameterDevModeIT extends RunAndCheckMojoTestBase {

    @Override
    protected int getPort() {
        return 8098;
    }

    protected void runAndCheck(boolean performCompile, String... options)
            throws MavenInvocationException, FileNotFoundException {
        run(performCompile, options);

        String resp = devModeClient.getHttpResponse();

        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application")
                .containsIgnoringCase("1.0-SNAPSHOT");

        // There's no json endpoints, so nothing else to check
    }

    @Test
    public void testThatTheTestsPassed() throws MavenInvocationException, IOException {
        //we also check continuous testing
        String executionDir = "projects/project-using-test-parameter-injection-processed";
        testDir = initProject("projects/project-using-test-parameter-injection", executionDir);
        runAndCheck();

        ContinuousTestingMavenTestUtils testingTestUtils = new ContinuousTestingMavenTestUtils(getPort());
        ContinuousTestingMavenTestUtils.TestStatus results = testingTestUtils.waitForNextCompletion();
        // This is a bit brittle when we add tests, but failures are often so catastrophic they're not even reported as failures,
        // so we need to check the pass count explicitly
        Assertions.assertEquals(0, results.getTestsFailed());
        Assertions.assertEquals(1, results.getTestsPassed());

        // Also check stdout out for errors of the form
        // 2025-05-12 15:58:46,729 WARNING [org.jun.jup.eng.con.InstantiatingConfigurationParameterConverter] (Test runner thread) Failed to load default class orderer class 'io.quarkus.test.config.QuarkusClassOrderer' set via the 'junit.jupiter.testclass.order.default' configuration parameter. Falling back to default behavior.: java.lang.ClassNotFoundException: io.quarkus.test.config.QuarkusClassOrderer
        //	at java.base/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:641)
        //	at java.base/jdk.internal.loader.ClassLoaders$AppClassLoader.loadClass(ClassLoaders.java:188)
        //	at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:526)
        // These are hard to test directly for but are not wanted
        String logs = running.log();
        assertThat(logs).doesNotContainIgnoringCase("java.lang.ClassNotFoundException")
                .doesNotContainIgnoringCase(QuarkusClassOrderer.class.getName());
    }

}
