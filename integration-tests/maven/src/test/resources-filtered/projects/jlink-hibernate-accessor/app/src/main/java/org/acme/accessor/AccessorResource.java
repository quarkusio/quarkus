package org.acme.accessor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.acme.model.api.Models;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hibernate.accessor.AccessorFactory;

import io.quarkus.hibernate.accessor.runtime.QuarkusAccessContext;

@Path("/accessors")
public class AccessorResource {
    private static final String GENERATED = "io.quarkus.hibernate.accessor.runtime.QuarkusHibernateAccessor";

    @Inject
    AccessorFactory factory;

    @GET
    @Path("ready")
    public String ready() {
        return "ready";
    }

    @GET
    public String verify() throws Exception {
        String strategy = ConfigProvider.getConfig().getValue("quarkus.hibernate-accessor.strategy", String.class);
        boolean generated = !strategy.equals("reflection");
        boolean reflection = !strategy.equals("generated");

        Module accessor = QuarkusAccessContext.class.getModule();
        Module application = getClass().getModule();

        require(accessor.isNamed(), "accessor must be in a named module");
        require(application.isNamed(), "application must be in a named module");
        require(accessor != application, "accessor must be separate from the application");

        if (generated) {
            assertGeneratedFactoryClassesPlacement(accessor);
        }

        exercise(factory, ApplicationModel.class);

        for (Class<?> type : new Class<?>[] { Models.publicType(), Models.hiddenType() }) {
            Module model = type.getModule();
            require(model.isNamed() && model != application && model != accessor, "model module isolation");
            equal("accessor.model", model.getName(), "model module name");
            Object instance = exercise(factory, type);
            equal("inherited", factory.valueReader(type.getSuperclass().getDeclaredField("inherited")).get(instance),
                    "inherited member");
            if (generated) {
                require(accessor.canRead(model), "accessor reads model");
                require(model.isExported(type.getPackageName(), accessor), "model exported to accessor");

                if (!Modifier.isPublic(type.getModifiers())) {
                    Class<?> bridge = Class.forName(type.getName() + "$$HibernateAccessorBridge", false, type.getClassLoader());
                    require(bridge.getModule() == model, "bridge placement");
                }
            }
            if (reflection) {
                require(model.isOpen(type.getPackageName(), accessor), "model open to accessor");
                require(model.isOpen(type.getSuperclass().getPackageName(), accessor), "declaring package open to accessor");
                // This unregistered field forces generated-with-fallback down its reflection path.
                equal("fallback", factory.valueReader(type.getDeclaredField("fallback")).get(instance), "reflection fallback");
            } else {
                require(!model.isOpen(type.getPackageName(), accessor), "generated access must not require opens");
            }
            require(!model.isExported(type.getPackageName(), application), "no export to unrelated application code");
            require(!model.isOpen(type.getPackageName(), application), "no open to unrelated application code");
            require(!type.getDeclaredField("value").trySetAccessible(), "unrelated reflection must remain denied");
        }
        return strategy + ":ok";
    }

    private void assertGeneratedFactoryClassesPlacement(Module accessor) throws ClassNotFoundException {
        Class<?> factoryType = factory.getClass();

        equal(GENERATED + "Factory", factoryType.getName(), "injected factory implementation");
        require(factoryType.getModule() == accessor, "factory placement");

        for (String suffix : new String[] { "ValueReaderImpl", "ValueWriterImpl", "InstantiatorImpl", "FactoryBootstrap" }) {
            Class<?> generatedFactoryClass = Class.forName(GENERATED + suffix, false, factoryType.getClassLoader());

            require(generatedFactoryClass.getModule() == accessor, "generated class placement: " + suffix);
        }
    }

    private static Object exercise(AccessorFactory factory, Class<?> type) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructor(String.class);
        Field field = type.getDeclaredField("value");
        Method setter = type.getDeclaredMethod("setValue", String.class);
        Method getter = type.getDeclaredMethod("getValue");

        Object instance = factory.instantiator(constructor).create("constructed");
        equal("constructed", factory.valueReader(field).get(instance), "private constructor and field read");

        factory.valueWriter(field).set(instance, "field-write");
        equal("field-write", factory.valueReader(getter).get(instance), "private getter");

        factory.valueWriter(setter).set(instance, "method-write");
        equal("method-write", factory.valueReader(field).get(instance), "private setter");
        return instance;
    }

    private static void equal(Object expected, Object actual, String operation) {
        require(Objects.equals(expected, actual), operation + ": expected " + expected + ", got " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
