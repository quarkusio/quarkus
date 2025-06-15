package io.quarkus.arc.runtime.logging;

import java.util.logging.Filter;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.logging.LogFilterFactory;

/**
 * Creates the implementation of the class by getting a bean from Arc. This class is loaded automatically by the
 * {@link java.util.ServiceLoader}.
 */
public class ArcLogFilterFactory implements LogFilterFactory {

    @Override
    public Filter create(String className) throws Exception {
        InstanceHandle<?> instance = Arc.container()
                .instance(Class.forName(className, true, Thread.currentThread().getContextClassLoader()));
        if (!instance.isAvailable()) {
            throw new IllegalStateException(
                    "Improper integration of '" + LogFilterFactory.class.getName() + "' detected");
        }
        return (Filter) instance.get();
    }

    @Override
    public int priority() {
        return LogFilterFactory.DEFAULT_PRIORITY - 100;
    }
}
