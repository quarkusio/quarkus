package io.quarkus.scala.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import io.quarkus.maven.it.RunAndCheckMojoTestBase;

public class ScalaDevModeIT extends RunAndCheckMojoTestBase {

    @Test
    public void testThatTheApplicationIsReloadedOnScalaChange() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic-scala", "projects/project-classic-run-scala-change");
        runAndCheck();

        // Edit the "Hello" message.
        File source = new File(testDir, "src/main/scala/org/acme/HelloResource.scala");
        String uuid = UUID.randomUUID().toString();
        filter(source, ImmutableMap.of("= \"hello\"", "= \"" + uuid + "\""));

        // Wait until we get "uuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> devModeClient.getHttpResponse("/app/hello").contains(uuid));

        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(source::isFile);

        filter(source, ImmutableMap.of(uuid, "carambar"));

        // Wait until we get "carambar"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> devModeClient.getHttpResponse("/app/hello").contains("carambar"));
    }

    @Test
    public void testExternalReloadableArtifacts() throws Exception {
        // Set up the external project
        final File externalJarDir = initProject("projects/external-reloadable-artifacts/external-lib");

        // Clean and install the external JAR in local repository (.m2)
        install(externalJarDir, true);

        // Set up the main project that uses the external dependency
        this.testDir = initProject("projects/external-reloadable-artifacts/app");

        // Run quarkus:dev process
        run(true);

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> devModeClient.getHttpResponse("/hello").contains("Hello"));

        final File greetingScala = externalJarDir.toPath().resolve("src").resolve("main")
                .resolve("scala").resolve("org").resolve("acme").resolve("lib")
                .resolve("Greeting.scala").toFile();
        assertThat(greetingScala).exists();

        // Uncomment the method bonjour() in Greeting.scala
        filter(greetingScala, Map.of("/*", "", "*/", ""));
        install(externalJarDir, false);

        final File greetingResourceScala = this.testDir.toPath().resolve("src").resolve("main")
                .resolve("scala").resolve("org").resolve("acme")
                .resolve("GreetingResource.scala").toFile();
        assertThat(greetingResourceScala).exists();

        // Update the GreetingResource.scala to call the Greeting.bonjour() method
        final String greetingBonjourCall = "Greeting.bonjour()";
        filter(greetingResourceScala, Map.of("Greeting.hello()", greetingBonjourCall));

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> devModeClient.getHttpResponse("/hello").contains("Bonjour"));

        // Change bonjour() method content in Greeting.java
        filter(greetingScala, Map.of("Bonjour", "Bonjour!"));
        install(externalJarDir, false);

        // Change GreetingResource.scala endpoint response to upper case letters
        filter(greetingResourceScala, Map.of(greetingBonjourCall, greetingBonjourCall.concat(".toUpperCase()")));

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> devModeClient.getHttpResponse("/hello").contains("BONJOUR!"));
    }
}