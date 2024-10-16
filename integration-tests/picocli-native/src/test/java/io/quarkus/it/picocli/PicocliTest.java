package io.quarkus.it.picocli;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
@QuarkusTestResource(PicocliTest.TestResource.class)
public class PicocliTest {

    private String value;

    @Test
    public void testExitCode(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("exitcode", "--code", Integer.toString(42));
        assertThat(result.exitCode()).isEqualTo(42);
        result = launcher.launch("exitcode", "--code", Integer.toString(0));
        assertThat(result.exitCode()).isEqualTo(0);
        result = launcher.launch("exitcode", "--code", Integer.toString(2));
        assertThat(result.exitCode()).isEqualTo(2);
    }

    @Test
    @Launch({ "test-command", "-f", "test.txt", "-f", "test2.txt", "-f", "test3.txt", "-s", "ERROR", "-h", "SOCKS=5.5.5.5",
            "-p", "privateValue", "pos1", "pos2" })
    public void testBasicReflection(LaunchResult result) throws UnknownHostException {
        assertThat(result.getOutput())
                .contains("-s", "ERROR")
                .contains("-p:privateValue")
                .contains("-p:privateValue")
                .contains("positional:[pos1, pos2]");

        assertThat(value).isNotNull();
    }

    @Test
    public void testMethodSubCommand(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("with-method-sub-command", "hello", "-n", "World!");
        assertThat(result.exitCode()).isZero();
        assertThat(result.getOutput()).contains("Hello World!");
        result = launcher.launch("with-method-sub-command", "goodBye", "-n", "Test?");
        assertThat(result.exitCode()).isZero();
        assertThat(result.getOutput()).contains("Goodbye Test?");
    }

    @Test
    public void testExcludeLogCapturing(QuarkusMainLauncher launcher) {
        org.jboss.logging.Logger.getLogger("test").error("error");
        LaunchResult result = launcher.launch("with-method-sub-command", "hello", "-n", "World!");
        assertThat(result.exitCode()).isZero();
        assertThat(result.getOutput()).contains("Hello World!");
    }

    @Test
    public void testIncludeLogCommand(QuarkusMainLauncher launcher) {
        org.jboss.logging.Logger.getLogger("test").error("error");
        LaunchResult result = launcher.launch("with-method-sub-command", "loggingHello", "-n", "World!");
        assertThat(result.exitCode()).isZero();
        assertThat(result.getOutput()).contains("ERROR [io.qua.it.pic.WithMethodSubCommand] (main) Hello World!");
        assertThat(result.getOutput()).doesNotContain("ERROR [test] (main) error");
    }

    @Test
    @Launch({ "command-used-as-parent", "-p", "testValue", "child" })
    public void testParentCommand(LaunchResult result) {
        assertThat(result.getOutput()).contains("testValue");

        assertThat(value).isNotNull();
    }

    @Test
    @Launch({ "exclusivedemo", "-b", "150" })
    public void testCommandWithArgGroup(LaunchResult result) {
        assertThat(result.getOutput())
                .contains("-a:0")
                .contains("-b:150")
                .contains("-c:0");

        assertThat(value).isNotNull();
    }

    @Test
    @Launch({ "dynamic-proxy" })
    public void testDynamicProxy(LaunchResult result) {
        assertThat(result.getOutput()).contains("2007-12-03T10:15:30");

        assertThat(value).isNotNull();
    }

    @Test
    @Launch("quarkus")
    public void testDynamicVersionProvider(LaunchResult launchResult) {
        assertThat(launchResult.getOutput()).contains("quarkus version 1.0");

        assertThat(value).isNotNull();
    }

    @Test
    @Launch({ "unmatched", "-x", "-a", "AAA", "More" })
    public void testUnmatched(LaunchResult launchResult) {
        assertThat(launchResult.getOutput())
                .contains("-a:AAA")
                .contains("-b:null")
                .contains("remainder:[More]")
                .contains("unmatched[-x]");

        assertThat(value).isNotNull();
    }

    @Test
    public void testI18s(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("localized-command-one", "--help");
        assertThat(result.getOutput())
                .contains("First in CommandOne");
        result = launcher.launch("localized-command-two", "--help");
        assertThat(result.getOutput())
                .contains("First in CommandTwo");
    }

    @Test
    @Launch({ "completion-reflection", "test" })
    public void testCompletionReflection() {

    }

    @Test
    @Launch("default-value-provider")
    public void testDefaultValueProvider(LaunchResult result) {
        assertThat(result.getOutput()).contains("default:default-value");

        assertThat(value).isNotNull();
    }

    public static class TestResource implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            return Collections.emptyMap();
        }

        @Override
        public void inject(TestInjector testInjector) {
            testInjector.injectIntoFields("dummy", f -> f.getName().equals("value"));
        }

        @Override
        public void stop() {

        }
    }

}
