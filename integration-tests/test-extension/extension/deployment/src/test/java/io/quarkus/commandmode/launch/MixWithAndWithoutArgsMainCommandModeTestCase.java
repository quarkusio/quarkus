package io.quarkus.commandmode.launch;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.test.QuarkusProdModeTest;

public class MixWithAndWithoutArgsMainCommandModeTestCase {
    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloWorldMain.class))
            .setApplicationName("run-exit")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true);

    @Test
    public void testRun() {
        Assertions.assertThat(config.getStartupConsoleOutput()).contains("Hello World");
        Assertions.assertThat(config.getExitCode()).isEqualTo(0);
    }

    @QuarkusMain
    public static class HelloWorldMain {

        static void main(String[] args) {
            System.out.println("Hello World");
        }

        void main() {
            System.out.println("Hi World");
        }
    }

}
