package io.quarkus.test.junit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.logging.QuarkusDelayedHandler;
import io.quarkus.dev.console.QuarkusConsole;

class MainTestLogRedirectTest {

    private final Logger rootLogger = LogContext.getLogContext().getLogger("");
    private final Logger applicationLogger = LogContext.getLogContext()
            .getLogger(MainTestLogRedirectTest.class.getName());

    private final QuarkusDelayedHandler delayedHandler = new QuarkusDelayedHandler();

    private Handler[] originalRootHandlers;
    private Level originalApplicationLoggerLevel;
    private PrintStream originalRedirectOut;
    private ByteArrayOutputStream redirectedOutput;

    @BeforeEach
    void captureLoggingState() {
        originalRootHandlers = rootLogger.getHandlers();
        originalApplicationLoggerLevel = applicationLogger.getLevel();
        originalRedirectOut = QuarkusConsole.REDIRECT_OUT;

        redirectedOutput = new ByteArrayOutputStream();
        QuarkusConsole.REDIRECT_OUT = new PrintStream(redirectedOutput, true, UTF_8);
        applicationLogger.setLevel(Level.INFO);

        // with console logging on, the build time logging setup leaves a console handler on the delayed handler, and
        // its formatter is what the redirect reuses; the level keeps this test off the real console
        ConsoleHandler consoleHandler = new ConsoleHandler(new PatternFormatter("%s%n"));
        consoleHandler.setLevel(Level.OFF);
        delayedHandler.setHandlers(new Handler[] { consoleHandler });
    }

    @AfterEach
    void restoreLoggingState() {
        rootLogger.setHandlers(originalRootHandlers);
        applicationLogger.setLevel(originalApplicationLoggerLevel);
        QuarkusConsole.REDIRECT_OUT = originalRedirectOut;
    }

    @Test
    void redirectsWhenTheDelayedHandlerIsNotOnTheRootLogger() {
        // Gradle 9.8.0 removes every handler from the root logger when the test worker starts
        rootLogger.setHandlers(new Handler[0]);
        MainTestLogRedirect redirect = new MainTestLogRedirect(delayedHandler);

        redirect.install();

        assertThat(rootLogger.getHandlers()).hasSize(1).doesNotContain(delayedHandler);
        assertThat(logAndCapture("captured without a handler on the root logger"))
                .containsOnlyOnce("captured without a handler on the root logger");

        redirect.uninstall();

        // the delayed handler was not on the root logger, so it must not be put there on the way out
        assertThat(rootLogger.getHandlers()).isEmpty();
    }

    @Test
    void redirectsWhenTheDelayedHandlerIsOnTheRootLogger() {
        rootLogger.setHandlers(new Handler[] { delayedHandler });
        MainTestLogRedirect redirect = new MainTestLogRedirect(delayedHandler);

        redirect.install();

        assertThat(rootLogger.getHandlers()).hasSize(1).doesNotContain(delayedHandler);
        assertThat(logAndCapture("captured with the delayed handler on the root logger"))
                .containsOnlyOnce("captured with the delayed handler on the root logger");

        redirect.uninstall();

        assertThat(rootLogger.getHandlers()).containsExactly(delayedHandler);
    }

    @Test
    void restoresTheRootLoggerAfterEveryLaunch() {
        rootLogger.setHandlers(new Handler[] { delayedHandler });

        for (int launch = 1; launch <= 2; launch++) {
            MainTestLogRedirect redirect = new MainTestLogRedirect(delayedHandler);

            redirect.install();

            assertThat(logAndCapture("launch " + launch)).containsOnlyOnce("launch " + launch);

            redirect.uninstall();

            assertThat(rootLogger.getHandlers()).containsExactly(delayedHandler);
        }
    }

    @Test
    void leavesALaunchStartedInsideAnotherOneToTheRedirectInPlace() {
        rootLogger.setHandlers(new Handler[] { delayedHandler });
        MainTestLogRedirect outer = new MainTestLogRedirect(delayedHandler);
        MainTestLogRedirect inner = new MainTestLogRedirect(delayedHandler);

        outer.install();
        inner.install();

        // a second redirect on the root logger would publish every record twice
        assertThat(rootLogger.getHandlers()).hasSize(1);
        assertThat(logAndCapture("logged once")).containsOnlyOnce("logged once");

        inner.uninstall();

        assertThat(rootLogger.getHandlers()).hasSize(1).doesNotContain(delayedHandler);

        outer.uninstall();

        assertThat(rootLogger.getHandlers()).containsExactly(delayedHandler);
    }

    @Test
    void doesNotRedirectWhenConsoleLoggingIsOff() {
        delayedHandler.clearHandlers();
        rootLogger.setHandlers(new Handler[] { delayedHandler });
        MainTestLogRedirect redirect = new MainTestLogRedirect(delayedHandler);

        redirect.install();

        assertThat(rootLogger.getHandlers()).containsExactly(delayedHandler);

        redirect.uninstall();

        assertThat(rootLogger.getHandlers()).containsExactly(delayedHandler);
    }

    private String logAndCapture(String message) {
        applicationLogger.info(message);
        QuarkusConsole.REDIRECT_OUT.flush();
        return redirectedOutput.toString(UTF_8);
    }
}
