package io.quarkus.runtime.logging;

import java.util.logging.Handler;
import java.util.logging.Level;

import org.graalvm.nativeimage.ImageInfo;
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
            return NO_HANDLERS;
        }
    }

    public static ConsoleHandler createDefaultHandler() {
        ConsoleHandler handler = new ConsoleHandler(new PatternFormatter("%d{HH:mm:ss,SSS} %-5p [%c{3.}] %s%e%n"));
        handler.setLevel(Level.INFO);
        return handler;
    }
}
