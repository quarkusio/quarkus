package io.quarkus.observability.devresource.testcontainers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.observability.common.config.ContainerConfig;
import io.quarkus.observability.devresource.Container;
import io.quarkus.observability.devresource.DevResourceLifecycleManager;

/**
 * A container resource abstraction
 */
public abstract class ContainerResource<T extends GenericContainer<T>, C extends ContainerConfig>
        implements DevResourceLifecycleManager<C> {

    protected T container;
    protected Container<C> wrapper;

    protected Container<C> set(T container) {
        this.container = container;
        this.wrapper = new TestcontainerContainer<>(container);
        return this.wrapper;
    }

    @Override
    public Map<String, String> start() {
        if (container == null) {
            set(defaultContainer());
        }
        container.start();
        return doStart();
    }

    protected <S> Map<String, Function<S, String>> createConfigProvider(
            Function<S, Map<String, String>> fn,
            Prop... props) {
        Map<String, Function<S, String>> map = new LinkedHashMap<>();
        for (Prop prop : props) {
            if (prop.predicate.get()) {
                String name = prop.name();
                map.put(name, o -> fn.apply(o).get(name));
            }
        }
        return map;
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }

    protected abstract T defaultContainer();

    protected abstract Map<String, String> doStart();

    public record Prop(
            String name,
            Supplier<Boolean> predicate) {
        public static Prop of(String name) {
            return new Prop(name, () -> true);
        }
    }
}
