package io.quarkus.hibernate.accessor.test.devmode;

import java.lang.reflect.Field;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.accessor.ValueReader;
import org.hibernate.accessor.ValueWriter;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * Exercises the generated accessors of {@link MmmReloadEntity} over HTTP so that a dev mode test can
 * trigger a live reload and then check that the accessors still target the right host class.
 */
@ApplicationScoped
public class ReloadAccessorEndpoint {

    void registerRoutes(@Observes Router router) {
        router.route("/accessor/read").handler(this::read);
        router.route("/accessor/write").handler(this::write);
    }

    private void read(RoutingContext context) {
        try {
            ValueReader<?> reader = accessorFactory().valueReader(nameField());
            context.response().end(String.valueOf(reader.get(new MmmReloadEntity("mmm"))));
        } catch (Throwable t) {
            respondWithFailure(context, t);
        }
    }

    private void write(RoutingContext context) {
        try {
            AccessorFactory factory = accessorFactory();
            Field field = nameField();
            ValueWriter writer = factory.valueWriter(field);
            ValueReader<?> reader = factory.valueReader(field);
            MmmReloadEntity entity = new MmmReloadEntity();
            writer.set(entity, "written");
            context.response().end(String.valueOf(reader.get(entity)));
        } catch (Throwable t) {
            respondWithFailure(context, t);
        }
    }

    private static Field nameField() throws NoSuchFieldException {
        return MmmReloadEntity.class.getDeclaredField("name");
    }

    private static AccessorFactory accessorFactory() throws Exception {
        Class<?> factoryClass = Thread.currentThread().getContextClassLoader()
                .loadClass("io.quarkus.hibernate.accessor.runtime.QuarkusHibernateAccessorFactory");
        return (AccessorFactory) factoryClass.getMethod("create").invoke(null);
    }

    private static void respondWithFailure(RoutingContext context, Throwable t) {
        context.response().setStatusCode(500).end(t.getClass().getName() + ": " + t.getMessage());
    }
}
