package io.quarkus.runtime.configuration;

import java.util.Collection;
import java.util.function.IntFunction;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.StringUtil;

/**
 *
 */
public final class ConfigUtils {
    private ConfigUtils() {
    }

    /**
     * This method replicates the logic of {@link SmallRyeConfig#getValues(String, Class, IntFunction)} for the given
     * default value string.
     *
     * @param config the config instance (must not be {@code null})
     * @param defaultValue the default value string (must not be {@code null})
     * @param itemType the item type class (must not be {@code null})
     * @param collectionFactory the collection factory (must not be {@code null})
     * @param <T> the item type
     * @param <C> the collection type
     * @return the collection (not {@code null})
     */
    public static <T, C extends Collection<T>> C getDefaults(SmallRyeConfig config, String defaultValue, Class<T> itemType,
            IntFunction<C> collectionFactory) {
        final String[] items = StringUtil.split(defaultValue);
        final C collection = collectionFactory.apply(items.length);
        for (String item : items) {
            collection.add(config.convert(item, itemType));
        }
        return collection;
    }
}
