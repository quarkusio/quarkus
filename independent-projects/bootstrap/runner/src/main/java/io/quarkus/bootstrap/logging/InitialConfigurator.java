package io.quarkus.bootstrap.logging;

import io.quarkus.bootstrap.graal.ImageInfo;
import io.quarkus.bootstrap.runner.Timing;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.jboss.logmanager.EmbeddedConfigurator;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.handlers.DelayedHandler;

/**
 *
 */
public final class InitialConfigurator implements EmbeddedConfigurator {

    public static final DelayedHandler DELAYED_HANDLER;

    static {
        //a hack around class loading
        //this is always loaded in the root class loader with jboss-logmanager,
        //however it may also be loaded in an isolated CL when running in dev
        //or test mode. If it is in an isolated CL we load the handler from
        //the class on the system class loader so they are equal
        //TODO: should this class go in its own module and be excluded from isolated class loading?
        DelayedHandler handler = new DelayedHandler();
        ClassLoader cl = InitialConfigurator.class.getClassLoader();
        try {
            Class<?> root = Class.forName(InitialConfigurator.class.getName(), false, ClassLoader.getSystemClassLoader());
            if (root.getClassLoader() != cl) {
                handler = (DelayedHandler) root.getDeclaredField("DELAYED_HANDLER").get(null);
            }

            if (!isQuarkusApplication(cl)) {
                // For tests, InitialConfigurator is called before Quarkus has come into play so we don't really know
                // if the application is a Quarkus application or not.
                // In order to avoid having log records queue indefinitely for tests that are not Quarkus tests,
                // the idea here is to check after a few seconds if any logging has been enabled
                // (which it will be if it's a Quarkus test), and if not, to simply drop all log records
                new Timer().schedule(new DropLogRecordersTask(cl), 5_000);
            }
        } catch (Exception e) {
            //ignore, happens on native image build
        }
        DELAYED_HANDLER = handler;
    }

    @Override
    public Level getMinimumLevelOf(final String loggerName) {
        return Level.ALL;
    }

    @Override
    public Level getLevelOf(final String loggerName) {
        return loggerName.isEmpty() ? Level.ALL : null;
    }

    @Override
    public Handler[] getHandlersOf(final String loggerName) {
        if (loggerName.isEmpty()) {
            if (ImageInfo.inImageBuildtimeCode()) {
                // we can't set a cleanup filter without the build items ready
                return new Handler[] {
                        createDefaultHandler()
                };
            } else {
                return new Handler[] { DELAYED_HANDLER };
            }
        } else {
            return EmbeddedConfigurator.NO_HANDLERS;
        }
    }

    public static ConsoleHandler createDefaultHandler() {
        ConsoleHandler handler = new ConsoleHandler(new PatternFormatter("%d{HH:mm:ss,SSS} %-5p [%c{3.}] %s%e%n"));
        handler.setLevel(Level.INFO);
        return handler;
    }

    private static boolean isQuarkusApplication(ClassLoader cl) {
        if (Timing.bootStartTime >= 0) { // prod mode
            return true;
        }
        if (forNameOrFalse(cl, "io.quarkus.runner.ApplicationImpl")) { // prod mode fallback
            return true;
        }
        if (cl.getResource("META-INF/dev-mode-context.dat") != null) { // dev mode
            return true;
        }
        return false;
    }

    private static boolean forNameOrFalse(ClassLoader cl, String name) {
        try {
            Class.forName(name, false, cl);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static class NoopHandler extends Handler {
        @Override
        public void publish(LogRecord record) {

        }

        @Override
        public void flush() {

        }

        @Override
        public void close() throws SecurityException {

        }
    }

    private static class DropLogRecordersTask extends TimerTask {

        private final ClassLoader cl;

        public DropLogRecordersTask(ClassLoader cl) {
            this.cl = cl;
        }

        @Override
        public void run() {
            if (DELAYED_HANDLER.isActivated()) {
                return;
            }
            // add a dummy handler in order to activate the delayed handler and thus dump the results
            // this still leaves the delayed handler with a large array of log records (as there is no
            // straightforward way to resize its Deque), but at least it doesn't grow
            DELAYED_HANDLER.addHandler(new NoopHandler());
        }
    }
}
