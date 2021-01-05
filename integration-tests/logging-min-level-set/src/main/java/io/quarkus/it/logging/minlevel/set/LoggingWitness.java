package io.quarkus.it.logging.minlevel.set;

import java.util.function.BiConsumer;

import org.jboss.logging.Logger;

public class LoggingWitness {

    public static boolean loggedError(String msg, Logger logger) {
        return logged(msg, logger::error);
    }

    public static boolean loggedWarn(String msg, Logger logger) {
        return logged(msg, logger::warn);
    }

    public static boolean loggedInfo(String msg, Logger logger) {
        return logged(msg, logger::info);
    }

    public static boolean loggedDebug(String msg, Logger logger) {
        return logged(msg, logger::debug);
    }

    public static boolean loggedTrace(String msg, Logger logger) {
        return logged(msg, logger::trace);
    }

    private static boolean logged(String msg, BiConsumer<String, Throwable> logFunction) {
        final LoggingWitnessThrowable throwable = new LoggingWitnessThrowable();
        logFunction.accept(msg, throwable);
        return throwable.logged;
    }

    private static final class LoggingWitnessThrowable extends Throwable {
        boolean logged;

        @Override
        public StackTraceElement[] getStackTrace() {
            logged = true;
            return new StackTraceElement[] {};
        }
    }
}
