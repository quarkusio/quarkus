package io.quarkus.kotlin.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.RunAndCheckMojoTestBase;

public class KotlinDevModeIT extends RunAndCheckMojoTestBase {

    @Test
    public void testThatTheApplicationIsReloadedOnKotlinChange() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic-kotlin", "projects/project-classic-run-kotlin-change");
        runAndCheck(false);

        // Edit the "Hello" message.
        File jaxRsResource = new File(testDir, "src/main/kotlin/org/acme/HelloResource.kt");
        String uuid = UUID.randomUUID().toString();
        filter(jaxRsResource, Map.of("return \"hello\"", "return \"" + uuid + "\""));

        // Wait until we get "uuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> devModeClient.getHttpResponse("/app/hello").contains(uuid));

        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(jaxRsResource::isFile);

        filter(jaxRsResource, Map.of(uuid, "carambar"));

        // Wait until we get "carambar"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> devModeClient.getHttpResponse("/app/hello").contains("carambar"));

        File greetingService = new File(testDir, "src/main/kotlin/org/acme/GreetingService.kt");
        String newUuid = UUID.randomUUID().toString();
        filter(greetingService, Map.of("\"hello\"", "\"" + newUuid + "\""));

        // Wait until we get "newUuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> devModeClient.getHttpResponse("/app/hello/bean").contains(newUuid));
    }

    @Test
    public void testThatTheApplicationIsReloadedOnKotlinChangeWithCustomCompilerArgs()
            throws MavenInvocationException, IOException {
        testDir = initProject("projects/kotlin-compiler-args", "projects/kotlin-compiler-args-change");
        runAndCheck(false);

        // Edit the "Hello" message.
        File jaxRsResource = new File(testDir, "src/main/kotlin/org/acme/HelloResource.kt");
        String uuid = UUID.randomUUID().toString();
        filter(jaxRsResource, Map.of("\"hello\"", "\"" + uuid + "\""));

        // Wait until we get "uuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> devModeClient.getHttpResponse("/app/hello").contains(uuid));
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> devModeClient.getHttpResponse("/graphql/schema.graphql").contains("[Banana!]!"));
    }

    @Test
    public void testExternalKotlinReloadableArtifacts() throws Exception {
        final String rootProjectPath = "projects/external-reloadable-artifacts";

        // Set up the external project
        final File externalJarDir = initProject(rootProjectPath + "/external-lib");

        // Clean and install the external JAR in local repository (.m2)
        install(externalJarDir, true);

        // Set up the main project that uses the external dependency
        this.testDir = initProject(rootProjectPath + "/app");

        // Run quarkus:dev process
        run(true);

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> devModeClient.getHttpResponse("/hello").contains("Hello"));

        final File greetingKotlin = externalJarDir.toPath().resolve("src").resolve("main")
                .resolve("kotlin").resolve("org").resolve("acme").resolve("lib")
                .resolve("Greeting.kt").toFile();
        assertThat(greetingKotlin).exists();

        // Uncomment the method bonjour() in Greeting.kt
        filter(greetingKotlin, Map.of("/*", "", "*/", ""));
        install(externalJarDir, false);

        final File greetingResourceKotlin = this.testDir.toPath().resolve("src").resolve("main")
                .resolve("kotlin").resolve("org").resolve("acme")
                .resolve("GreetingResource.kt").toFile();
        assertThat(greetingResourceKotlin).exists();

        // Update the GreetingResource.kt to call the Greeting.bonjour() method
        final String greetingBonjourCall = "Greeting.bonjour()";
        filter(greetingResourceKotlin, Map.of("Greeting.hello()", greetingBonjourCall));

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> devModeClient.getHttpResponse("/hello").contains("Bonjour"));

        // Change bonjour() method content in Greeting.kt
        filter(greetingKotlin, Map.of("Bonjour", "Bonjour!"));
        install(externalJarDir, false);

        // Change GreetingResource.kt endpoint response to upper case letters
        filter(greetingResourceKotlin, Map.of(greetingBonjourCall, greetingBonjourCall.concat(".uppercase()")));

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> devModeClient.getHttpResponse("/hello").contains("BONJOUR!"));
    }
}