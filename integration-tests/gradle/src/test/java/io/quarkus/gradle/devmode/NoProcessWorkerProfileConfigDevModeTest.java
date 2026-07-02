package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for https://github.com/quarkusio/quarkus/issues/55131.
 *
 * <p>
 * The project sets {@code gradle.quarkus.gradle-worker.no-process=true}, so Quarkus workers run in-process inside the
 * Gradle daemon (class-loader isolation). {@code greeting.message} is defined only in
 * {@code application-dev.properties}, so a {@code /hello} response proves the profile-aware config file was loaded for
 * the active dev profile. Before the fix, the in-process worker scrubbed the daemon's system properties, the dev
 * profile was dropped, and the application failed to start with {@code SRCFG00014}.
 */
public class NoProcessWorkerProfileConfigDevModeTest extends QuarkusDevGradleTestBase {
    @Override
    protected String projectDirectoryName() {
        return "no-process-worker-profile-config";
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).contains("hello-from-dev-profile");
    }
}
