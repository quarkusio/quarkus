package io.quarkus.commandmode.launch;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.test.QuarkusProdModeTest;

public class PrivateStaticMainCommandModeTestCase {
    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloWorldMain.class))
            .setApplicationName("run-exit")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectedException(RuntimeException.class);

    @Test
    public void shouldNeverRun() {
        fail();
    }

    @QuarkusMain
    public static class HelloWorldMain {

        private static void main(String[] args) {
            System.out.println("Hello World");
        }
    }

}
