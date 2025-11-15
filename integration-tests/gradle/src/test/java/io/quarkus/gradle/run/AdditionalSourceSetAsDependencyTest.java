package io.quarkus.gradle.run;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.gradle.devmode.QuarkusDevGradleTestBase;

// Reproduces https://github.com/quarkusio/quarkus/issues/48768.
// Despite extending QuarkusDevGradleTestBase, this test does not use the dev mode,
// but rather runs the application in a production mode using `quarkusRun` task.
// At this point it's the only test that does so, but if more such tests are added,
// perhaps it would make sense to create a separate base class for them.
public class AdditionalSourceSetAsDependencyTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "additional-source-set-as-dependency";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", "quarkusRun" };
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).contains("Hello from Quarkus REST...");
    }
}
