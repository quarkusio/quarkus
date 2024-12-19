package io.quarkus.flyway.deployment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.flywaydb.core.api.callback.Callback;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

/**
 * Logic to locate and process Flyway {@link Callback} classes.
 * This class also helps to keep the {@link FlywayProcessor} class as lean as possible to make it easier to maintain
 */
class FlywayCallbacksLocator {
    private final Collection<FlywayBuildItem> flywayBuildItems;
    private final CombinedIndexBuildItem combinedIndexBuildItem;
    private final BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer;

    private FlywayCallbacksLocator(Collection<FlywayBuildItem> flywayBuildItems,
            CombinedIndexBuildItem combinedIndexBuildItem, BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer) {
        this.flywayBuildItems = flywayBuildItems;
        this.combinedIndexBuildItem = combinedIndexBuildItem;
        this.reflectiveClassProducer = reflectiveClassProducer;
    }

    public static FlywayCallbacksLocator with(Collection<FlywayBuildItem> flywayBuildItems,
            CombinedIndexBuildItem combinedIndexBuildItem, BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer) {
        return new FlywayCallbacksLocator(flywayBuildItems, combinedIndexBuildItem, reflectiveClassProducer);
    }

    /**
     * Main logic to identify callbacks and return them to be processed by the {@link FlywayProcessor}
     *
     * @return Map containing the callbacks for each datasource. The datasource name is the map key
     * @exception ClassNotFoundException if the {@link Callback} class cannot be located by the Quarkus class loader
     * @exception InstantiationException if the {@link Callback} class represents an abstract class.
     * @exception InvocationTargetException if the underlying constructor throws an exception.
     * @exception IllegalAccessException if the {@link Callback} constructor is enforcing Java language access control
     *            and the underlying constructor is inaccessible
     */
    public Map<String, Collection<Callback>> getCallbacks()
            throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final Map<String, Collection<Callback>> callbacks = new HashMap<>();
        for (FlywayBuildItem flywayBuildItem : flywayBuildItems) {
            final Collection<Callback> instances = callbacksFor(flywayBuildItem);
            callbacks.put(flywayBuildItem.getId(), instances);
        }
        return callbacks;
    }

    /**
     *
     * Reads the configuration, instantiates the {@link Callback} class. Also, adds it to the reflective producer
     *
     * @return List of callbacks for the datasource
     * @exception ClassNotFoundException if the {@link Callback} class cannot be located by the Quarkus class loader
     * @exception InstantiationException if the {@link Callback} class represents an abstract class.
     * @exception InvocationTargetException if the underlying constructor throws an exception.
     * @exception IllegalAccessException if the {@link Callback} constructor is enforcing Java language access control
     *            and the underlying constructor is inaccessible
     */
    private Collection<Callback> callbacksFor(FlywayBuildItem flywayBuildItem)
            throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException {
        final Optional<List<String>> callbackConfig = flywayBuildItem.getBuildTimeConfig().callbacks;
        if (!callbackConfig.isPresent()) {
            return Collections.emptyList();
        }
        final Collection<String> callbacks = callbackConfig.get();
        final Collection<Callback> instances = new ArrayList<>(callbacks.size());
        for (String callback : callbacks) {
            final ClassInfo clazz = combinedIndexBuildItem.getIndex().getClassByName(DotName.createSimple(callback));
            Objects.requireNonNull(clazz,
                    "Flyway callback not found, please verify the fully qualified name for the class: " + callback);
            if (Modifier.isAbstract(clazz.flags()) || !clazz.hasNoArgsConstructor()) {
                throw new IllegalArgumentException(
                        "Invalid Flyway callback. It shouldn't be abstract and must have a default constructor");
            }
            final Class<?> clazzType = Class.forName(callback, false, Thread.currentThread().getContextClassLoader());
            final Callback instance = (Callback) clazzType.getConstructors()[0].newInstance();
            instances.add(instance);
            reflectiveClassProducer
                    .produce(ReflectiveClassBuildItem.builder(clazz.name().toString()).build());
        }
        return instances;
    }
}
