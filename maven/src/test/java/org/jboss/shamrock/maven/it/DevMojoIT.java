package org.jboss.shamrock.maven.it;

import com.google.common.collect.ImmutableMap;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.jboss.shamrock.maven.it.verifier.RunningInvoker;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class DevMojoIT extends MojoTestBase {

    private RunningInvoker running;
    private File testDir;

    @After
    public void cleanup() {
        System.out.println("Done");
        if (running != null) {
            running.stop();
        }
        awaitUntilServerDown();
    }

    @Test
    public void testThatClassAppCanRun() throws MavenInvocationException, FileNotFoundException, InterruptedException {
        testDir = initProject("projects/classic", "projects/project-classic-run");
        assertThat(testDir).isDirectory();
        running = new RunningInvoker(testDir, false);
        running.execute(Arrays.asList("compile", "shamrock:dev"), Collections.emptyMap());

        String resp = getHttpResponse();
        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application").containsIgnoringCase("org.acme")
                .containsIgnoringCase("1.0-SNAPSHOT");

        String greeting = getHttpResponse("/app/hello");
        assertThat(greeting).containsIgnoringCase("hello");
    }

    @Test
    public void testThatTheApplicationIsReloadedOnJavaChange() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-java-change");
        assertThat(testDir).isDirectory();
        running = new RunningInvoker(testDir, false);
        running.execute(Arrays.asList("compile", "shamrock:dev"), Collections.emptyMap());

        String resp = getHttpResponse();

        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application").containsIgnoringCase("org.acme")
                .containsIgnoringCase("1.0-SNAPSHOT");

        String greeting = getHttpResponse("/app/hello");
        assertThat(greeting).containsIgnoringCase("hello");

        // Edit the "Hello" message.
        File source = new File(testDir, "src/main/java/org/acme/HelloResource.java");
        String uuid = UUID.randomUUID().toString();
        filter(source, ImmutableMap.of("return \"hello\";", "return \"" + uuid + "\";"));

        // Wait until we get "uuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse("/app/hello").contains(uuid));
    }

    @Test
    public void testThatTheApplicationIsReloadedOnConfigChange() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-config-change");
        assertThat(testDir).isDirectory();
        running = new RunningInvoker(testDir, false);
        running.execute(Arrays.asList("compile", "shamrock:dev"), Collections.emptyMap());

        String resp = getHttpResponse();

        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application").containsIgnoringCase("org.acme")
                .containsIgnoringCase("1.0-SNAPSHOT");

        String greeting = getHttpResponse("/app/hello/greeting");
        assertThat(greeting).containsIgnoringCase("bonjour");

        // Edit the "Hello" message.
        File source = new File(testDir, "src/main/resources/META-INF/microprofile-config.properties");
        String uuid = UUID.randomUUID().toString();
        filter(source, ImmutableMap.of("bonjour", uuid));

        // Wait until we get "uuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse("/app/hello/greeting").contains(uuid));
    }

}
