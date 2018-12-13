package org.jboss.shamrock.maven.it;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.jboss.shamrock.maven.it.verifier.MavenProcessInvocationResult;
import org.jboss.shamrock.maven.it.verifier.RunningInvoker;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
        if (running != null) {
            running.stop();
        }
        awaitUntilServerDown();
    }

    @Test
    public void testThatClassAppCanRun() throws MavenInvocationException, FileNotFoundException {
        testDir = initProject("projects/classic", "projects/project-classic-run");
        runAndCheck();
    }

    @Test
    public void testThatTheApplicationIsReloadedOnJavaChange() throws MavenInvocationException, IOException, InterruptedException {
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
        sleep();
        filter(source, ImmutableMap.of(uuid, "carambar"));

        // Wait until we get "uuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse("/app/hello").contains("carambar"));
    }

    @Test
    public void testThatTheApplicationIsReloadedOnNewResource() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-new-resource");
        runAndCheck();

        File source = new File(testDir, "src/main/java/org/acme/MyNewResource.java");
        String myNewResource =
                "package org.acme;\n" +
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

    @Test
    @Ignore("Issue https://github.com/protean-project/shamrock/issues/245")
    public void testThatTheApplicationIsReloadedOnNewServlet() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-new-servlet");
        runAndCheck();

        File source = new File(testDir, "src/main/java/org/acme/MySimpleServlet.java");
        String myNewServlet =
                "package org.acme;\n" +
                        "\n" +
                        "import java.io.IOException;\n" +
                        "\n" +
                        "import javax.servlet.annotation.WebServlet;\n" +
                        "import javax.servlet.http.HttpServlet;\n" +
                        "import javax.servlet.http.HttpServletRequest;\n" +
                        "import javax.servlet.http.HttpServletResponse;\n" +
                        "\n" +
                        "@WebServlet(name=\"MyServlet\", urlPatterns=\"/my\")\n" +
                        "public class MySimpleServlet extends HttpServlet {\n" +
                        "    \n" +
                        "    @Override\n" +
                        "    protected void doGet(HttpServletRequest request, \n" +
                        "      HttpServletResponse response) throws IOException {\n" +
                        "          response.setContentType(\"text/plain\");\n" +
                        "          response.getWriter().println(\"hello from servlet\");\n" +
                        "      }\n" +
                        "}";
        FileUtils.write(source, myNewServlet, Charset.forName("UTF-8"));

        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse("/my").contains("hello from servlet"));

        // delete servlet
        sleep();
        source.delete();

        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse("/my", 404));

    }

    private void runAndCheck() throws FileNotFoundException, MavenInvocationException {
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

        sleep();
        sleep();
        File source = new File(testDir, "src/main/resources/META-INF/microprofile-config.properties");
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
        testDir = initProject("projects/classic", "projects/project-classic-run-resource-change");
        runAndCheck();

        // Create a new resource
        File source = new File(testDir, "src/main/resources/META-INF/resources/lorem.txt");
        FileUtils.write(source, "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.", "UTF-8");
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
            String content = getHttpResponse("/app/hello");
            last.set(content);
            return content.contains(uuid);
        });

        assertThat(last.get()).containsIgnoringCase("error")
                .containsIgnoringCase("return \"" + uuid + "\"")
                .containsIgnoringCase("compile");

        sleep();
        filter(source, ImmutableMap.of("\"" +uuid + "\"", "\"carambar\";"));

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

        sleep();

        filter(source, ImmutableMap.of("message", "foobarbaz"));

        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse("/app/hello").contains("foobarbaz"));
    }



    @Test
    public void testErrorMessageWhenNoJavaSources() throws IOException, MavenInvocationException {
        testDir = initProject("projects/classic", "projects/project-no-sources");
        FileUtils.deleteQuietly(new File(testDir, "src/main/java"));
        running = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = running.execute(Arrays.asList("compile", "shamrock:dev"), Collections.emptyMap());
        await().until(() -> result.getProcess() != null  && ! result.getProcess().isAlive());
        assertThat(running.log()).containsIgnoringCase("BUILD FAILURE");
    }

    @Test
    public void testErrorMessageWhenNoTarget() throws IOException, MavenInvocationException {
        testDir = initProject("projects/classic", "projects/project-no-target");
        FileUtils.deleteQuietly(new File(testDir, "target"));
        running = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = running.execute(Collections.singletonList("shamrock:dev"), Collections.emptyMap());
        await().until(() -> result.getProcess() != null  && ! result.getProcess().isAlive());
        assertThat(running.log()).containsIgnoringCase("BUILD FAILURE");
    }

    @Test
    public void testErrorMessageWhenNoTargetClasses() throws IOException, MavenInvocationException {
        testDir = initProject("projects/classic", "projects/project-no-classes");
        new File(testDir, "target").mkdirs();
        // Be sure we don't have classes.
        FileUtils.deleteQuietly(new File(testDir, "target/classes"));

        running = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = running.execute(Collections.singletonList("shamrock:dev"), Collections.emptyMap());
        await().until(() -> result.getProcess() != null  && ! result.getProcess().isAlive());
        assertThat(running.log()).containsIgnoringCase("BUILD FAILURE");
    }

}
