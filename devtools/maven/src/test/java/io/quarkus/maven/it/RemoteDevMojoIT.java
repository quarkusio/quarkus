package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import io.quarkus.maven.it.verifier.RunningInvoker;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class RemoteDevMojoIT extends MojoTestBase {

    private RunningInvoker running;
    private RunningInvoker runningAgent;
    private File testDir;
    private File agentDir;

    @AfterEach
    public void cleanup() throws IOException {
        if (running != null) {
            running.stop();
        }
        if (runningAgent != null) {
            runningAgent.stop();
        }
        awaitUntilServerDown();
    }

    @Test
    public void testThatTheApplicationIsReloadedOnJavaChange()
            throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/classic", "projects/project-classic-run-java-change-remote");
        agentDir = initProject("projects/classic", "projects/project-classic-run-java-change-local");
        runAndCheck();

        // Edit the "Hello" message.
        File source = new File(agentDir, "src/main/java/org/acme/HelloResource.java");
        String uuid = UUID.randomUUID().toString();
        filter(source, ImmutableMap.of("return \"hello\";", "return \"" + uuid + "\";"));

        // Wait until we get "uuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse("/app/hello").contains(uuid));

        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(source::isFile);

        filter(source, ImmutableMap.of(uuid, "carambar"));

        // Wait until we get "carambar"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse("/app/hello").contains("carambar"));
    }

    @Override
    protected String getBrokenReason() {
        if (running != null && !running.getResult().getProcess().isAlive()) {
            try {
                return running.log();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (runningAgent != null && !runningAgent.getResult().getProcess().isAlive()) {
            try {
                return runningAgent.log();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Test
    public void testThatTheApplicationIsReloadedOnKotlinChange()
            throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/classic-kotlin", "projects/project-classic-run-kotlin-change-remote");
        agentDir = initProject("projects/classic-kotlin", "projects/project-classic-run-kotlin-change-local");
        runAndCheck();

        // Edit the "Hello" message.
        File source = new File(agentDir, "src/main/kotlin/org/acme/HelloResource.kt");
        String uuid = UUID.randomUUID().toString();
        filter(source, ImmutableMap.of("return \"hello\"", "return \"" + uuid + "\""));

        // Wait until we get "uuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse("/app/hello").contains(uuid));

        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(source::isFile);

        filter(source, ImmutableMap.of(uuid, "carambar"));

        // Wait until we get "carambar"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse("/app/hello").contains("carambar"));
    }

    @Test
    public void testThatTheApplicationIsReloadedOnNewResource() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-new-resource-remote");
        agentDir = initProject("projects/classic", "projects/project-classic-run-new-resource-local");
        runAndCheck();

        File source = new File(agentDir, "src/main/java/org/acme/MyNewResource.java");
        String myNewResource = "package org.acme;\n" +
                "\n" +
                "import javax.ws.rs.GET;\n" +
                "import javax.ws.rs.Path;\n" +
                "import javax.ws.rs.Produces;\n" +
                "import javax.ws.rs.core.MediaType;\n" +
                "\n" +
                "@Path(\"/foo\")\n" +
                "public class MyNewResource {\n" +

                "    @GET\n" +
                "    @Produces(MediaType.TEXT_PLAIN)\n" +
                "    public String foo() {\n" +
                "        return \"bar\";\n" +
                "    }\n" +
                "}\n";
        FileUtils.write(source, myNewResource, Charset.forName("UTF-8"));

        // Wait until we get "bar"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse("/app/foo").contains("bar"));
    }

    private void runAndCheck() throws FileNotFoundException, MavenInvocationException {
        assertThat(testDir).isDirectory();
        running = new RunningInvoker(testDir, false);
        running.execute(Arrays.asList("compile", "quarkus:dev"), Collections.emptyMap());

        String resp = getHttpResponse();

        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application").containsIgnoringCase("org.acme")
                .containsIgnoringCase("1.0-SNAPSHOT");

        String greeting = getHttpResponse("/app/hello");
        assertThat(greeting).containsIgnoringCase("hello");

        try {
            //file granularity is 1s on some platforms
            //we check for changes since agent start
            //so we need to make sure we start at least 1s after copying the files
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        runningAgent = new RunningInvoker(agentDir, false);
        runningAgent.execute(Arrays.asList("compile", "quarkus:remote-dev"), Collections.emptyMap());

        try {
            Thread.sleep(1000);
            await().pollDelay(100, TimeUnit.MILLISECONDS)
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .atMost(1, TimeUnit.MINUTES)
                    .until(() -> runningAgent.log().contains("Connected to remote server"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void testThatTheApplicationIsReloadedOnConfigChange() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-config-change-remote");
        agentDir = initProject("projects/classic", "projects/project-classic-run-config-change-local");
        assertThat(testDir).isDirectory();
        running = new RunningInvoker(testDir, false);
        running.execute(Arrays.asList("compile", "quarkus:dev"), Collections.emptyMap());

        String resp = getHttpResponse();
        runningAgent = new RunningInvoker(agentDir, false);
        runningAgent.execute(Arrays.asList("compile", "quarkus:remote-dev"), Collections.emptyMap());

        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application").containsIgnoringCase("org.acme")
                .containsIgnoringCase("1.0-SNAPSHOT");

        String greeting = getHttpResponse("/app/hello/greeting");
        assertThat(greeting).containsIgnoringCase("bonjour");

        File source = new File(agentDir, "src/main/resources/application.properties");
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(source::isFile);

        String uuid = UUID.randomUUID().toString();
        filter(source, ImmutableMap.of("bonjour", uuid));

        // Wait until we get "uuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> getHttpResponse("/app/hello/greeting").contains(uuid));
    }

    @Test
    public void testThatNewResourcesAreServed() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-resource-change-remote");
        agentDir = initProject("projects/classic", "projects/project-classic-run-resource-change-local");
        runAndCheck();

        // Create a new resource
        File source = new File(agentDir, "src/main/resources/META-INF/resources/lorem.txt");
        FileUtils.write(source,
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                "UTF-8");
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> getHttpResponse("/lorem.txt").contains("Lorem ipsum"));

        // Update the resource
        String uuid = UUID.randomUUID().toString();
        FileUtils.write(source, uuid, "UTF-8");
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> getHttpResponse("/lorem.txt").contains(uuid));

        // Delete the resource
        //TODO: not supported yet in remote dev
        //        source.delete();
        //        await()
        //                .pollDelay(1, TimeUnit.SECONDS)
        //                .atMost(1, TimeUnit.MINUTES)
        //                .until(() -> getHttpResponse("/lorem.txt", 404));
    }

    @Test
    public void testThatApplicationRecoversCompilationIssue() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-compilation-issue-remote");
        agentDir = initProject("projects/classic", "projects/project-classic-run-compilation-issue-local");
        runAndCheck();

        // Edit the "Hello" message.
        File source = new File(agentDir, "src/main/java/org/acme/HelloResource.java");
        String uuid = UUID.randomUUID().toString();
        filter(source, ImmutableMap.of("return \"hello\";", "return \"" + uuid + "\"")); // No semi-colon

        // Wait until we get "uuid"
        AtomicReference<String> last = new AtomicReference<>();
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> {
                    String content = getHttpResponse("/app/hello", true);
                    last.set(content);
                    return content.contains(uuid);
                });

        assertThat(last.get()).containsIgnoringCase("error")
                .containsIgnoringCase("return \"" + uuid + "\"")
                .containsIgnoringCase("compile");

        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(source::isFile);
        filter(source, ImmutableMap.of("\"" + uuid + "\"", "\"carambar\";"));

        // Wait until we get "uuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse("/app/hello").contains("carambar"));
    }

    @Test
    public void testThatNewBeanAreDiscovered() throws IOException, MavenInvocationException {
        testDir = initProject("projects/classic", "projects/project-classic-run-new-bean-remote");
        agentDir = initProject("projects/classic", "projects/project-classic-run-run-new-bean-local");
        runAndCheck();

        // Edit the "Hello" message.
        File source = new File(agentDir, "src/main/java/org/acme/MyBean.java");
        String content = "package org.acme;\n" +
                "\n" +
                "import javax.enterprise.context.ApplicationScoped;\n" +
                "\n" +
                "@ApplicationScoped\n" +
                "public class MyBean {\n" +
                "\n" +
                "    public String get() {\n" +
                "        return \"message\";\n" +
                "    }\n" +
                "    \n" +
                "}";
        FileUtils.write(source, content, "UTF-8");

        // Update the resource ot use the bean
        File resource = new File(agentDir, "src/main/java/org/acme/HelloResource.java");
        filter(resource, Collections.singletonMap("String greeting;", "String greeting;\n @Inject MyBean bean;"));
        filter(resource, Collections.singletonMap("\"hello\"", "bean.get()"));

        // Wait until we get "uuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse("/app/hello").contains("message"));

        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(source::isFile);

        filter(source, ImmutableMap.of("message", "foobarbaz"));

        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse("/app/hello").contains("foobarbaz"));
    }

}
