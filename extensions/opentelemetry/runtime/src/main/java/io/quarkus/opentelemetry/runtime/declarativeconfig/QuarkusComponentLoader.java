package io.quarkus.opentelemetry.runtime.declarativeconfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.AutoConfigureListener;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.ConfigurableComponentProvider;

/**
 * A {@link ComponentLoader} that adds Quarkus {@link ComponentProvider} instances to the ones its delegate loads.
 * <p>
 * For {@link ComponentProvider}, {@link #load(Class)} returns, in this order:
 * </p>
 * <ol>
 * <li>the Quarkus providers</li>
 * <li>a {@link ConfigurableComponentProvider} for each autoconfigure {@code Configurable*Provider} the delegate
 * loads, so components written for autoconfigure resolve by the same names and read the same flat {@code otel.*}
 * properties</li>
 * <li>the delegate's providers</li>
 * </ol>
 * <p>
 * A provider is left out when an earlier one has the same type and name, so a Quarkus or autoconfigure provider
 * never conflicts with a same-named delegate provider. The list is built on the first call, and later calls return
 * the same instances. Every other SPI is loaded by the delegate on each call. Each loaded {@link ResourceProvider}
 * that implements {@link AutoConfigureListener} is recorded, as autoconfigure records the SPI instances it loads.
 * Listeners of other SPIs are left to the code that loads them.
 * </p>
 * <p>
 * Instances are not thread-safe; the SDK is built on one thread.
 * </p>
 */
final class QuarkusComponentLoader implements ComponentLoader {

    // The SPIs Quarkus loads through this loader outside the declarative configuration. Upstream's
    // DeclarativeConfiguration calls the listeners of what it loads, so recording those would call them twice.
    private static final Set<Class<?>> LISTENER_SPIS = Set.of(ResourceProvider.class);

    private final ComponentLoader delegate;
    private final ConfigProperties otelConfigProperties;
    private final List<ComponentProvider> quarkusProviders = new ArrayList<>();
    private final Set<AutoConfigureListener> listeners = Collections.newSetFromMap(new IdentityHashMap<>());
    private List<ComponentProvider> componentProviders;

    /**
     * Creates a loader.
     *
     * @param delegate the loader for every SPI, and for the {@link ComponentProvider} instances not replaced by a
     *        Quarkus provider
     * @param otelConfigProperties the flat {@code otel.*} properties passed to the autoconfigure providers
     * @param quarkusProviders the Quarkus providers, which win over every other provider of the same type and name
     */
    QuarkusComponentLoader(ComponentLoader delegate, ConfigProperties otelConfigProperties,
            List<ComponentProvider> quarkusProviders) {
        this.delegate = delegate;
        this.otelConfigProperties = otelConfigProperties;
        this.quarkusProviders.addAll(quarkusProviders);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Iterable<T> load(Class<T> spiClass) {
        if (spiClass == ComponentProvider.class) {
            return (Iterable<T>) componentProviders();
        }
        List<T> loaded = ComponentLoader.loadList(delegate, spiClass);
        if (!LISTENER_SPIS.contains(spiClass)) {
            return loaded;
        }
        for (T instance : loaded) {
            if (instance instanceof AutoConfigureListener listener) {
                listeners.add(listener);
            }
        }
        return loaded;
    }

    @Override
    public String toString() {
        return "QuarkusComponentLoader{delegate=" + delegate + '}';
    }

    /**
     * Adds a Quarkus provider, which wins over every other provider of the same type and name.
     *
     * @param provider the provider, not {@code null}
     * @throws IllegalStateException if the {@link ComponentProvider} instances were already loaded
     */
    void addComponentProvider(ComponentProvider provider) {
        if (componentProviders != null) {
            throw new IllegalStateException("The component providers were already loaded");
        }
        quarkusProviders.add(provider);
    }

    /**
     * Returns the {@link ResourceProvider} instances loaded so far that implement {@link AutoConfigureListener}.
     *
     * @return a snapshot of the listeners, or an empty list
     */
    List<AutoConfigureListener> listeners() {
        return List.copyOf(listeners);
    }

    // Built once: a provider instance must be the same one each time it is loaded, because AutoConfigureListener
    // callbacks reach the instances that created the components.
    private List<ComponentProvider> componentProviders() {
        if (componentProviders == null) {
            List<ComponentProvider> result = new ArrayList<>(quarkusProviders);
            addUnclaimed(result, ConfigurableComponentProvider.adaptAll(delegate, otelConfigProperties));
            addUnclaimed(result, delegate.load(ComponentProvider.class));
            componentProviders = List.copyOf(result);
        }
        return componentProviders;
    }

    private static void addUnclaimed(List<ComponentProvider> result, Iterable<ComponentProvider> providers) {
        for (ComponentProvider provider : providers) {
            if (!claimed(result, provider)) {
                result.add(provider);
            }
        }
    }

    private static boolean claimed(List<ComponentProvider> providers, ComponentProvider candidate) {
        for (ComponentProvider provider : providers) {
            if (provider.getType() == candidate.getType() && provider.getName().equals(candidate.getName())) {
                return true;
            }
        }
        return false;
    }
}
