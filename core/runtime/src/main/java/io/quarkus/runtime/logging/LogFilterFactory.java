package io.quarkus.runtime.logging;

import java.util.ServiceLoader;
import java.util.logging.Filter;

/**
 * Factory that allows for the creation of {@link Filter} classes annotated with {@link io.quarkus.logging.LoggingFilter}.
 * Implementations of this class are loaded via the {@link ServiceLoader} and the implementation selected is the one
 * with the lowest value returned from the {@code priority} method.
 */
public interface LogFilterFactory {

    int MIN_PRIORITY = Integer.MAX_VALUE;
    int DEFAULT_PRIORITY = 0;

    Filter create(String className) throws Exception;

    default int priority() {
        return DEFAULT_PRIORITY;
    }

    static LogFilterFactory load() {
        LogFilterFactory result = null;
        ServiceLoader<LogFilterFactory> load = ServiceLoader.load(LogFilterFactory.class);
        for (LogFilterFactory next : load) {
            if (result == null) {
                result = next;
            } else {
                if (next.priority() < result.priority()) {
                    result = next;
                }
            }
        }
        if (result == null) {
            result = new ReflectionLogFilterFactory();
        }
        return result;
    }

    /**
     * The default implementation used when no other implementation is found.
     * This simply calls the class' no-arg constructor (and fails if one does not exist).
     */
    class ReflectionLogFilterFactory implements LogFilterFactory {

        @Override
        public Filter create(String className) throws Exception {
            return (Filter) Class.forName(className, true, Thread.currentThread().getContextClassLoader())
                    .getConstructor().newInstance();
        }

        @Override
        public int priority() {
            return LogFilterFactory.MIN_PRIORITY;
        }
    }
}
