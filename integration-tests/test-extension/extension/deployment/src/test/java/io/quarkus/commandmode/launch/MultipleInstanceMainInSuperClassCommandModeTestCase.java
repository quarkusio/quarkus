package io.quarkus.commandmode.launch;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.test.QuarkusProdModeTest;

public class MultipleInstanceMainInSuperClassCommandModeTestCase {
    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloWorldSuperSuper.class, HelloWorldSuper.class, HelloWorldMain.class))
            .setApplicationName("run-exit")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true);

    @Test
    public void testRun() {
        Assertions.assertThat(config.getStartupConsoleOutput()).contains("Hi World");
        Assertions.assertThat(config.getExitCode()).isEqualTo(0);
    }

    @QuarkusMain
    public static class HelloWorldMain extends HelloWorldSuper {

    }

    public static class HelloWorldSuperSuper {

        protected void main(String[] args) {
            System.out.println("Hi World");
        }

        protected void main() {
            System.out.println("Hello World");
        }
    }

    public static class HelloWorldSuper extends HelloWorldSuperSuper {

        protected void main2() {
            System.out.println("Hello");
        }
    }

}
