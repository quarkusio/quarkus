package io.quarkus.maven.it;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

import io.quarkus.test.devmode.util.DevModeClient;

/**
 * This test has been isolated as it is very flaky and causing issues with Develocity PTS.
 */
@DisableForNative
public class FlakyDevMojoIT extends RunAndCheckMojoTestBase {

    protected DevModeClient devModeClient = new DevModeClient(getPort());

    @Test
    public void testThatNewResourcesAreServed() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic-with-log", "projects/project-classic-run-resource-change");
        runAndCheck();

        // Create a new resource
        Path source = testDir.toPath().resolve("src/main/resources/META-INF/resources/lorem.txt");
        Files.writeString(source,
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(TestUtils.getDefaultTimeout(), TimeUnit.MINUTES)
                .until(() -> devModeClient.getHttpResponse("/lorem.txt"), containsString("Lorem ipsum"));

        // Update the resource
        String uuid = UUID.randomUUID().toString();
        Files.writeString(source, uuid);
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(TestUtils.getDefaultTimeout(), TimeUnit.MINUTES)
                .until(() -> devModeClient.getHttpResponse("/lorem.txt"), equalTo(uuid));

        // Delete the resource
        Files.delete(source);
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(TestUtils.getDefaultTimeout(), TimeUnit.MINUTES)
                .until(() -> devModeClient.getHttpResponse("/lorem.txt", 404));
    }
}
