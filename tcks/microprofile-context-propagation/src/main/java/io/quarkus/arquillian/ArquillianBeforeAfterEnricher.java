package io.quarkus.arquillian;

import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import io.quarkus.arc.Arc;

/**
 * Activates request context before test runs and shuts it down afterwards
 */
public class ArquillianBeforeAfterEnricher {

    private static final String ERROR_MSG = "Arc container is not running, cannot activate CDI contexts!";

    @Inject
    @DeploymentScoped
    private InstanceProducer<ClassLoader> appClassloader;

    public void on(@Observes(precedence = -100) org.jboss.arquillian.test.spi.event.suite.Before event) throws Throwable {
        //we are outside the runtime class loader, so we don't have direct access to the container
        Class<?> arcClz = appClassloader.get().loadClass(Arc.class.getName());
        Object container = arcClz.getMethod("container").invoke(null);
        if (container != null) {
            boolean running = (boolean) container.getClass().getMethod("isRunning").invoke(container);
            if (running) {
                Object context = container.getClass().getMethod("requestContext").invoke(container);
                context.getClass().getMethod("activate").invoke(context);
            } else {
                throw new IllegalStateException(ERROR_MSG);
            }
        }
    }

    public void on(@Observes(precedence = 100) org.jboss.arquillian.test.spi.event.suite.After event) throws Throwable {
        Class<?> arcClz = appClassloader.get().loadClass(Arc.class.getName());
        Object container = arcClz.getMethod("container").invoke(null);
        if (container != null) {
            boolean running = (boolean) container.getClass().getMethod("isRunning").invoke(container);
            if (running) {
                Object context = container.getClass().getMethod("requestContext").invoke(container);
                context.getClass().getMethod("terminate").invoke(context);
            } else {
                throw new IllegalStateException(ERROR_MSG);
            }
        }
    }
}
