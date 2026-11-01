package io.quarkus.test.junit;

import java.io.OutputStream;
import java.util.logging.Formatter;

import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.handlers.OutputStreamHandler;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.bootstrap.logging.QuarkusDelayedHandler;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.test.junit.main.LaunchResult;

/**
 * Sends the log output of a launched main application to {@link QuarkusConsole#REDIRECT_OUT} for the duration of one
 * launch, so the lines the application logs are captured in the {@link LaunchResult} the test sees.
 * <p>
 * {@link #install()} reads {@link QuarkusConsole#REDIRECT_OUT} as it runs, so it belongs after
 * {@link QuarkusConsole#installRedirects()} and {@link #uninstall()} before that stream is closed again. One instance
 * covers one launch and is not thread safe.
 */
final class MainTestLogRedirect {

    private final QuarkusDelayedHandler fallbackHandler;

    private QuarkusDelayedHandler replacedHandler;
    private RedirectHandler redirectHandler;

    MainTestLogRedirect() {
        this(InitialConfigurator.DELAYED_HANDLER);
    }

    MainTestLogRedirect(QuarkusDelayedHandler fallbackHandler) {
        this.fallbackHandler = fallbackHandler;
    }

    void install() {
        var rootLogger = LogContext.getLogContext()
                .getLogger("");

        for (var handler : rootLogger.getHandlers()) {
            if (handler instanceof RedirectHandler) {
                // a launch that is still running, an Aesh one for instance, already redirects the root logger
                return;
            }
        }

        QuarkusDelayedHandler installedHandler = null;
        for (var handler : rootLogger.getHandlers()) {
            if (handler instanceof QuarkusDelayedHandler delayedHandler) {
                installedHandler = delayedHandler;
                break;
            }
        }

        // Some test runners detach the handlers Quarkus installed at bootstrap from the root logger before the test
        // worker starts, Gradle 9.8.0 among them (gradle/gradle#39359). The delayed handler still holds the console
        // configuration in that case, so take the formatter from it rather than skipping the redirect.
        Formatter formatter = consoleFormatterOf(installedHandler != null ? installedHandler : fallbackHandler);
        if (formatter == null) {
            // console logging is switched off, so there is no console output to mirror into the LaunchResult
            return;
        }

        redirectHandler = new RedirectHandler(QuarkusConsole.REDIRECT_OUT, formatter);
        replacedHandler = installedHandler;
        if (replacedHandler != null) {
            rootLogger.removeHandler(replacedHandler);
        }
        rootLogger.addHandler(redirectHandler);
    }

    void uninstall() {
        if (redirectHandler == null) {
            return;
        }
        var rootLogger = LogContext.getLogContext()
                .getLogger("");
        rootLogger.removeHandler(redirectHandler);
        if (replacedHandler != null) {
            rootLogger.addHandler(replacedHandler);
        }
        redirectHandler = null;
        replacedHandler = null;
    }

    private static Formatter consoleFormatterOf(QuarkusDelayedHandler delayedHandler) {
        for (var handler : delayedHandler.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                return handler.getFormatter();
            }
        }
        return null;
    }

    /**
     * Marks the handler as this class's own, so a launch starting while another one still runs leaves the redirect
     * already in place alone instead of adding a second one and publishing every record twice.
     */
    private static final class RedirectHandler extends OutputStreamHandler {

        RedirectHandler(OutputStream out, Formatter formatter) {
            super(out, formatter);
        }
    }
}
