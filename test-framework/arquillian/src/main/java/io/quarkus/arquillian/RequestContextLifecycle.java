package io.quarkus.arquillian;

import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;

/**
 * Activates request context before test runs and shuts it down afterwards
 */
public class RequestContextLifecycle {

    private static final Logger LOGGER = Logger.getLogger(RequestContextLifecycle.class);

    private static final int DEFAULT_PRECEDENCE = 100;

    @Inject
    @DeploymentScoped
    private InstanceProducer<ClassLoader> appClassloader;

    public void on(@Observes(precedence = DEFAULT_PRECEDENCE) Before event) throws Throwable {
        //we are outside the runtime class loader, so we don't have direct access to the container
        ClassLoader classLoader = appClassloader.get();
        if (classLoader != null) {
            Class<?> arcClz = classLoader.loadClass(Arc.class.getName());
            Object container = arcClz.getMethod("container").invoke(null);
            if (container != null) {
                boolean running = (boolean) container.getClass().getMethod("isRunning").invoke(container);
                if (running) {
                    Object context = container.getClass().getMethod("requestContext").invoke(container);
                    context.getClass().getMethod("activate").invoke(context);
                    LOGGER.debug("RequestContextLifecycle activating CDI Request context.");
                }
            }
        }
    }

    public void on(@Observes(precedence = DEFAULT_PRECEDENCE) After event) throws Throwable {
        //we are outside the runtime class loader, so we don't have direct access to the container
        ClassLoader classLoader = appClassloader.get();
        if (classLoader != null) {
            Class<?> arcClz = classLoader.loadClass(Arc.class.getName());
            Object container = arcClz.getMethod("container").invoke(null);
            if (container != null) {
                boolean running = (boolean) container.getClass().getMethod("isRunning").invoke(container);
                if (running) {
                    Object context = container.getClass().getMethod("requestContext").invoke(container);
                    context.getClass().getMethod("terminate").invoke(context);
                    LOGGER.debug("RequestContextLifecycle activating CDI Request context.");
                }
            }
        }
    }
}
