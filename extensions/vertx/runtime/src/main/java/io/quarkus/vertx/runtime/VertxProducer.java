package io.quarkus.vertx.runtime;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;

/**
 * Expose the Vert.x event bus and produces Mutiny instances.
 * <p>
 * The original Vert.x instance is coming from the core artifact.
 *
 * IMPL NOTE: There is no need to cache the mutiny locally because the bean instances are stored in the
 * singleton context, i.e. the producer method is only called once.
 */
@ApplicationScoped
public class VertxProducer {

    private static final Logger LOGGER = Logger.getLogger(VertxProducer.class);

    @Singleton
    @Produces
    public EventBus eventbus(Vertx vertx) {
        return vertx.eventBus();
    }

    @Singleton
    @Produces
    public io.vertx.mutiny.core.Vertx mutiny(Vertx vertx) {
        return io.vertx.mutiny.core.Vertx.newInstance(vertx);
    }

    @Singleton
    @Produces
    public io.vertx.mutiny.core.eventbus.EventBus mutinyEventBus(io.vertx.mutiny.core.Vertx mutiny) {
        return mutiny.eventBus();
    }

    /**
     * Undeploy verticles backed by contextual instances of {@link ApplicationScoped} beans before the application context is
     * destroyed. Otherwise, Vertx may attempt to stop the verticles after the CDI container is shut down.
     *
     * @param event
     * @param beanManager
     */
    void undeployVerticles(@Observes @BeforeDestroyed(ApplicationScoped.class) Object event, BeanManager beanManager) {
        // Only beans with the AbstractVerticle in the set of bean types are considered - we need a deployment id
        Set<Bean<?>> beans = beanManager.getBeans(AbstractVerticle.class, Any.Literal.INSTANCE);
        Context applicationContext = beanManager.getContext(ApplicationScoped.class);
        for (Bean<?> bean : beans) {
            if (ApplicationScoped.class.equals(bean.getScope())) {
                // Only beans with @ApplicationScoped are considered
                Object instance = applicationContext.get(bean);
                if (instance != null) {
                    // Only existing instances are considered
                    try {
                        AbstractVerticle verticle = (AbstractVerticle) instance;
                        io.vertx.mutiny.core.Vertx mutiny = beanManager.createInstance()
                                .select(io.vertx.mutiny.core.Vertx.class).get();
                        mutiny.undeploy(verticle.deploymentID()).await().indefinitely();
                        LOGGER.debugf("Undeployed verticle: %s", instance.getClass());
                    } catch (Exception e) {
                        // In theory, a user can undeploy the verticle manually
                        LOGGER.debugf("Unable to undeploy verticle %s: %s", instance.getClass(), e.toString());
                    }
                }
            }
        }
    }

}
