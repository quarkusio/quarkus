package io.quarkus.commandmode.launch;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.test.QuarkusProdModeTest;

public class InstanceMainInSuperClassNoArgsCommandModeTestCase {
    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloWorldSuper.class, HelloWorldMain.class))
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
    public static class HelloWorldMain extends HelloWorldSuper {

    }

    public static class HelloWorldSuper {

        void main() {
            System.out.println("Hello World");
        }

        void main2() {
            System.out.println("Hello");
        }

        void main3(String[] args) {
            System.out.println("Hello");
        }
    }

}
