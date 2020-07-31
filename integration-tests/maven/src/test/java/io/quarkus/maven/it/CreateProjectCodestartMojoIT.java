package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.InvokerLogger;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.deployment.util.ProcessUtil;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.test.devmode.util.DevModeTestUtils;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@DisableForNative
public class CreateProjectCodestartMojoIT extends QuarkusPlatformAwareMojoTestBase {

    private static final Logger LOG = Logger.getLogger(CreateProjectCodestartMojoIT.class.getName());

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Invoker invoker;
    private RunningInvoker running;
    private File testDir;

    private static final String GRADLE_WRAPPER_WINDOWS = "gradlew.bat";
    private static final String GRADLE_WRAPPER_UNIX = "gradlew";
    private static final String GRADLE_NO_DAEMON = "--no-daemon";

    private void check(final File resource, final String contentsToFind) throws IOException {
        assertThat(resource).isFile();
        assertThat(FileUtils.readFileToString(resource, "UTF-8")).contains(contentsToFind);
    }

    @AfterEach
    public void cleanup() {
        if (running != null) {
            running.stop();
        }
        executor.shutdownNow();
    }

    @ParameterizedTest
    @CsvSource({ "maven,java", "maven,kotlin", "maven,scala", "gradle,java", "gradle,kotlin", "gradle,scala" })
    public void generateProjectRunTestsAndDev(String buildtool, String language) throws Exception {
        testDir = prepareTestDir("project-" + buildtool + "-" + language);
        LOG.info("creating project in " + testDir.toPath().toString());
        runCreateCommand(buildtool, "resteasy,qute" + (!Objects.equals(language, "java") ? "," + language : ""));
        LOG.info("running quarkus test and dev command...");
        switch (buildtool) {
            case "maven":
                runMavenQuarkusDevCommand();
                LOG.info("checking resteasy...");
                checkRestEasyDevmode();

                break;
            case "gradle":
                // FIXME find a way to make gradle check work
                //runGradleQuarkusDevCommand();
                break;
        }
    }

    private static File prepareTestDir(String name) {
        File tc = new File("target/codestart-test/" + name);
        if (tc.isDirectory()) {
            try {
                FileUtils.deleteDirectory(tc);
            } catch (IOException e) {
                throw new RuntimeException("Cannot delete directory: " + tc, e);
            }
        }
        boolean mkdirs = tc.mkdirs();
        LOG.log(Level.FINE, "codestart-test created? %s", mkdirs);
        return tc;
    }

    private void checkRestEasyDevmode() {
        String resp = DevModeTestUtils.getHttpResponse();

        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application").containsIgnoringCase("org.test")
                .containsIgnoringCase("1.0-SNAPSHOT");

        String greeting = DevModeTestUtils.getHttpResponse("/resteasy/hello");
        assertThat(greeting).containsIgnoringCase("hello");
    }

    private void runCreateCommand(String buildTool, String extensions)
            throws MavenInvocationException, FileNotFoundException, UnsupportedEncodingException {
        // Scaffold the new project
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);

        Properties properties = new Properties();
        properties.put("projectGroupId", "org.test");
        properties.put("projectArtifactId", "my-test-app");
        properties.put("codestartsEnabled", "true");
        properties.put("withExampleCode", "true");
        properties.put("buildTool", buildTool);
        properties.put("extensions", extensions);

        InvocationResult result = executeCreate(properties);

        assertThat(result.getExitCode()).isZero();

        // Run
        // As the directory is not empty (log) navigate to the artifactID directory
        testDir = new File(testDir, "my-test-app");
    }

    private void runMavenQuarkusDevCommand() throws MavenInvocationException {
        running = new RunningInvoker(testDir, false);
        final Properties mvnRunProps = new Properties();
        mvnRunProps.setProperty("debug", "false");
        running.execute(Arrays.asList("test", "compile", "quarkus:dev"), Collections.emptyMap(), mvnRunProps);
    }

    private void runGradleQuarkusDevCommand() throws IOException, InterruptedException {
        runGradleWrapper(testDir, "test", "quarkusDev");
    }

    private InvocationResult executeCreate(Properties params)
            throws MavenInvocationException, FileNotFoundException, UnsupportedEncodingException {

        params.setProperty("platformGroupId", ToolsConstants.IO_QUARKUS);
        params.setProperty("platformArtifactId", "quarkus-bom");
        params.setProperty("platformVersion", getPluginVersion());

        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setGoals(Collections.singletonList(
                getPluginGroupId() + ":" + getPluginArtifactId() + ":" + getPluginVersion() + ":create"));
        request.setDebug(false);
        request.setShowErrors(false);
        request.setProperties(params);
        getEnv().forEach(request::addShellEnvironment);
        File log = new File(testDir, "build-create-codestart-" + testDir.getName() + ".log");
        PrintStreamLogger logger = new PrintStreamLogger(new PrintStream(new FileOutputStream(log), false, "UTF-8"),
                InvokerLogger.DEBUG);
        invoker.setLogger(logger);
        return invoker.execute(request);
    }

    public void runGradleWrapper(File projectDir, String... args) throws IOException, InterruptedException {
        List<String> command = new LinkedList<>();
        command.add(projectDir.toPath().resolve(getGradleWrapperName()).toAbsolutePath().toString());
        command.add(GRADLE_NO_DAEMON);
        command.addAll(Arrays.asList(args));
        executor.submit(() -> {
            try {
                System.out.println("Running command: " + command);
                final Process p = new ProcessBuilder()
                        .directory(projectDir)
                        .command(command)
                        .start();
                try {
                    ProcessUtil.streamToSysOutSysErr(p);
                    p.waitFor(3, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    p.destroyForcibly();
                    Thread.currentThread().interrupt();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private String getGradleWrapperName() {
        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows")) {
            return GRADLE_WRAPPER_WINDOWS;
        }
        return GRADLE_WRAPPER_UNIX;
    }

}
