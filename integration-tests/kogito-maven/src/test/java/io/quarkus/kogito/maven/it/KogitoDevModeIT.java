package io.quarkus.kogito.maven.it;

import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.RunAndCheckMojoTestBase;

public class KogitoDevModeIT extends RunAndCheckMojoTestBase {

    @Test
    public void testThatTheKogitoApplicationRuns() throws MavenInvocationException, IOException {
        testDir = initProject("projects/simple-kogito", "projects/project-classic-run-kogito");
        run();

        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse("/persons").equals("[]"));

    }
}
