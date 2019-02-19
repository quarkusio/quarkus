package io.quarkus.security.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.logmanager.EmbeddedConfigurator;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;

/**
 * Add verbose logging using the log manager EmbeddedConfigurator service interface
 */
public class DebugEmbeddedConfigurator implements EmbeddedConfigurator {
    static String[] excludes = {"org.jboss.threads", "org.xnio", "org.apache.commons", "org.jboss.shrinkwrap", "javax.management"};
    static HashSet<String> excludesSet = new HashSet<>(Arrays.asList(excludes));

    private Handler[] handlers;


    public DebugEmbeddedConfigurator() {
        Formatter fmt = new PatternFormatter("%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n");
        ConsoleHandler consoleHandler = new ConsoleHandler(fmt);
        handlers = new Handler[]{consoleHandler};
    }
    @Override
    public Level getMinimumLevelOf(String loggerName) {
        if(excludesSet.stream().anyMatch(loggerName::startsWith))
            return Level.INFO;
        return Level.FINER;
    }

    @Override
    public Level getLevelOf(String loggerName) {
        if(excludesSet.stream().anyMatch(loggerName::startsWith))
            return Level.INFO;
        return Level.FINEST;
    }

    @Override
    public Handler[] getHandlersOf(String loggerName) {
        return handlers;
    }
}
