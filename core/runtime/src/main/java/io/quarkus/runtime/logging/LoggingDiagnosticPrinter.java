package io.quarkus.runtime.logging;

import java.io.PrintStream;
import java.util.Enumeration;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;

/**
 *
 */
public final class LoggingDiagnosticPrinter {
    public static void printDiagnostics(PrintStream w) {
        final Enumeration<String> loggerNames = LogContext.getLogContext().getLoggerNames();
        while (loggerNames.hasMoreElements()) {
            final Logger logger = Logger.getLogger(loggerNames.nextElement());
            w.printf("Logger \"%s\"%n", logger.getName());
            final Level level = logger.getLevel();
            w.printf("  Level: %s (effective %d)%n", level == null ? "(inherited)" : level,
                    Integer.valueOf(logger.getEffectiveLevel()));
            final Filter filter = logger.getFilter();
            if (filter != null) {
                w.printf("  Filter: %s%n", filter);
            }
            w.printf("  Parent filters %s%n", logger.getUseParentFilters() ? "enabled" : "disabled");
            final Handler[] handlers = logger.getHandlers();
            if (handlers.length > 0) {
                w.println("  Handlers:");
                printHandlers(handlers, w, 2);
            }
        }
    }

    private static void printHandlers(final Handler[] handlers, final PrintStream w, final int depth) {
        for (int i = 0; i < handlers.length; i++) {
            indent(w, depth);
            final Handler handler = handlers[i];
            w.printf("Handler: %s%n", handler);
            final Filter filter = handler.getFilter();
            if (filter != null) {
                indent(w, depth + 1);
                w.printf("Filter: %s%n", filter);
            }
            final String encoding = handler.getEncoding();
            indent(w, depth + 1);
            w.printf("Encoding: %s%n", encoding);
            final Level level = handler.getLevel();
            indent(w, depth + 1);
            w.printf("Level: %s%n", level);
            final Formatter formatter = handler.getFormatter();
            if (formatter != null) {
                indent(w, depth + 1);
                w.printf("Formatter: %s%n", formatter);
            }
            if (handler instanceof ExtHandler) {
                final ExtHandler extHandler = (ExtHandler) handler;
                indent(w, depth + 1);
                w.printf("Flags: enabled=%s auto-flush=%s caller-calculation=%s%n",
                        Boolean.valueOf(extHandler.isEnabled()),
                        Boolean.valueOf(extHandler.isAutoFlush()),
                        Boolean.valueOf(extHandler.isCallerCalculationRequired()));
                final Handler[] subHandlers = extHandler.getHandlers();
                if (subHandlers.length > 0) {
                    indent(w, depth + 1);
                    w.println("Nested handlers:");
                    printHandlers(subHandlers, w, depth + 2);
                }
            }
        }
    }

    private static void indent(final PrintStream w, final int depth) {
        for (int j = 0; j < depth; j++) {
            w.print("  ");
        }
    }
}
