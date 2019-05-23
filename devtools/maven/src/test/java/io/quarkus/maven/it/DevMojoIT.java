package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class DevMojoIT extends MojoTestBase {

    private RunningInvoker running;
    private File testDir;

    @AfterEach
    public void cleanup() {
        if (running != null) {
            running.stop();
        }
        awaitUntilServerDown();
    }

    @Test
    public void testThatClassAppCanRun() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run");
        runAndCheck();

        //make sure that the Class.getPackage() works for app classes
        String pkg = getHttpResponse("/app/hello/package");
        assertThat(pkg).isEqualTo("org.acme");

        //make sure webjars work
        getHttpResponse("webjars/bootstrap/3.1.0/css/bootstrap.min.css");
        assertThatOutputWorksCorrectly(running.log());
    }

    @Test
    public void testThatTheApplicationIsReloadedOnJavaChange() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-java-change");
        runAndCheck();

        // Edit the "Hello" message.
        File source = new File(testDir, "src/main/java/org/acme/HelloResource.java");
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

    @Test
    public void testThatTheApplicationIsReloadedMultiModule() throws MavenInvocationException, IOException {
        testDir = initProject("projects/multimodule", "projects/multimodule-with-deps");
        runAndCheck();

        // Edit the "Hello" message.
        File source = new File(testDir, "rest/src/main/java/org/acme/HelloResource.java");
        final String uuid = UUID.randomUUID().toString();
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

        // Create a new resource
        source = new File(testDir, "html/src/main/resources/META-INF/resources/lorem.txt");
        FileUtils.write(source,
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                "UTF-8");
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> getHttpResponse("/lorem.txt").contains("Lorem ipsum"));

        // Update the resource
        FileUtils.write(source, uuid, "UTF-8");
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> getHttpResponse("/lorem.txt").contains(uuid));

        // Delete the resource
        source.delete();
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> getHttpResponse("/lorem.txt", 404));
    }

    @Test
    public void testMultiModuleDevModeWithLocalDepsDisabled() throws MavenInvocationException, IOException {
        testDir = initProject("projects/multimodule", "projects/multimodule-nodeps");
        runAndCheck("-DnoDeps");

        String greeting = getHttpResponse("/app/hello/greeting");
        assertThat(greeting).containsIgnoringCase("bonjour");

        // Edit the "Hello" message.
        File source = new File(testDir, "rest/src/main/java/org/acme/HelloResource.java");
        filter(source, ImmutableMap.of("return \"hello\";", "return \"" + UUID.randomUUID().toString() + "\";"));

        // Edit the greeting property.
        source = new File(testDir, "runner/src/main/resources/application.properties");
        final String uuid = UUID.randomUUID().toString();
        filter(source, ImmutableMap.of("greeting=bonjour", "greeting=" + uuid + ""));

        // Wait until we get "uuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse("/app/hello/greeting").contains(uuid));

        greeting = getHttpResponse("/app/hello");
        assertThat(greeting).containsIgnoringCase("hello");
    }

    @Test
    public void testThatTheApplicationIsReloadedOnKotlinChange() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic-kotlin", "projects/project-classic-run-kotlin-change");
        runAndCheck();

        // Edit the "Hello" message.
        File source = new File(testDir, "src/main/kotlin/org/acme/HelloResource.kt");
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
        testDir = initProject("projects/classic", "projects/project-classic-run-new-resource");
        runAndCheck();

        File source = new File(testDir, "src/main/java/org/acme/MyNewResource.java");
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

    private void runAndCheck(String... options) throws FileNotFoundException, MavenInvocationException {
        assertThat(testDir).isDirectory();
        running = new RunningInvoker(testDir, false);
        final List<String> args = new ArrayList<>(2 + options.length);
        args.add("compile");
        args.add("quarkus:dev");
        if (options.length > 0) {
            for (String s : options) {
                args.add(s);
            }
        }
        running.execute(args, Collections.emptyMap());

        String resp = getHttpResponse();

        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application").containsIgnoringCase("org.acme")
                .containsIgnoringCase("1.0-SNAPSHOT");

        String greeting = getHttpResponse("/app/hello");
        assertThat(greeting).containsIgnoringCase("hello");
    }

    @Test
    public void testThatTheApplicationIsReloadedOnConfigChange() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-config-change");
        assertThat(testDir).isDirectory();
        running = new RunningInvoker(testDir, false);
        running.execute(Arrays.asList("compile", "quarkus:dev"), Collections.emptyMap());

        String resp = getHttpResponse();

        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application").containsIgnoringCase("org.acme")
                .containsIgnoringCase("1.0-SNAPSHOT");

        String greeting = getHttpResponse("/app/hello/greeting");
        assertThat(greeting).containsIgnoringCase("bonjour");

        File source = new File(testDir, "src/main/resources/application.properties");
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
    public void testThatAddingConfigFileWorksCorrectly() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic-noconfig", "projects/project-classic-run-noconfig-add-config");
        assertThat(testDir).isDirectory();
        running = new RunningInvoker(testDir, false);
        running.execute(Arrays.asList("compile", "quarkus:dev"), Collections.emptyMap());

        String resp = getHttpResponse();

        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application").containsIgnoringCase("org.acme")
                .containsIgnoringCase("1.0-SNAPSHOT");

        String greeting = getHttpResponse("/app/hello/greeting");
        assertThat(greeting).contains("initialValue");

        File configurationFile = new File(testDir, "src/main/resources/application.properties");
        assertThat(configurationFile).doesNotExist();

        String uuid = UUID.randomUUID().toString();

        FileUtils.write(configurationFile,
                "greeting=" + uuid,
                "UTF-8");
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(configurationFile::isFile);

        // Wait until we get "uuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> getHttpResponse("/app/hello/greeting").contains(uuid));
    }

    @Test
    public void testThatNewResourcesAreServed() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-resource-change");
        runAndCheck();

        // Create a new resource
        File source = new File(testDir, "src/main/resources/META-INF/resources/lorem.txt");
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
        source.delete();
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> getHttpResponse("/lorem.txt", 404));
    }

    @Test
    public void testThatApplicationRecoversCompilationIssue() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-compilation-issue");
        runAndCheck();

        // Edit the "Hello" message.
        File source = new File(testDir, "src/main/java/org/acme/HelloResource.java");
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
        testDir = initProject("projects/classic", "projects/project-classic-run-new-bean");
        runAndCheck();

        // Edit the "Hello" message.
        File source = new File(testDir, "src/main/java/org/acme/MyBean.java");
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
        File resource = new File(testDir, "src/main/java/org/acme/HelloResource.java");
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

    @Test
    public void testErrorMessageWhenNoTarget() throws IOException, MavenInvocationException {
        testDir = initProject("projects/classic", "projects/project-no-target");
        FileUtils.deleteQuietly(new File(testDir, "target"));
        running = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = running.execute(Collections.singletonList("quarkus:dev"), Collections.emptyMap());
        await().until(() -> result.getProcess() != null && !result.getProcess().isAlive());
        assertThat(running.log()).containsIgnoringCase("BUILD FAILURE");
    }

    @Test
    public void testErrorMessageWhenNoTargetClasses() throws IOException, MavenInvocationException {
        testDir = initProject("projects/classic", "projects/project-no-classes");
        new File(testDir, "target").mkdirs();
        // Be sure we don't have classes.
        FileUtils.deleteQuietly(new File(testDir, "target/classes"));

        running = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = running.execute(Collections.singletonList("quarkus:dev"), Collections.emptyMap());
        await().until(() -> result.getProcess() != null && !result.getProcess().isAlive());
        assertThat(running.log()).containsIgnoringCase("BUILD FAILURE");
    }
}
