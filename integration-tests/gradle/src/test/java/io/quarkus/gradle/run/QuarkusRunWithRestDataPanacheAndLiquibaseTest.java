package io.quarkus.gradle.run;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.gradle.devmode.QuarkusDevGradleTestBase;

// Reproduces https://github.com/quarkusio/quarkus/issues/54001.
// Despite extending QuarkusDevGradleTestBase, this test does not use dev mode,
// but rather runs the application in production mode using the `quarkusRun` task.
// The combination of `hibernate-orm-rest-data-panache` and `liquibase` triggered
// the regression introduced when `quarkusRun` was bootstrapping the app with
// `QuarkusBootstrap.Mode.TEST` (see #48950 and `46ce666581d` for the Maven-side fix).
public class QuarkusRunWithRestDataPanacheAndLiquibaseTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "quarkus-run-mode-panache-liquibase";
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
