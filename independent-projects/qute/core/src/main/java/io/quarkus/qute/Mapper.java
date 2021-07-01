package io.quarkus.qute;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Maps keys to values in a similar way to {@link java.util.Map}. The difference is that a mapper could be stateless, i.e. the
 * lookup may be performed dynamically.
 * 
 * @see ValueResolvers#mapperResolver()
 */
public interface Mapper {

    default Object get(String key) {
        return null;
    }

    default CompletionStage<Object> getAsync(String key) {
        return CompletedStage.of(get(key));
    }

    /**
     * 
     * @param key
     * @return {@code true} if the mapper should be applied to the specified key
     */
    default boolean appliesTo(String key) {
        return true;
    }

    /**
     * 
     * @param map
     * @return a mapper that wraps the given map
     */
    static Mapper wrap(Map<String, ?> map) {
        return new Mapper() {

            @Override
            public boolean appliesTo(String key) {
                return map.containsKey(key);
            }

            @Override
            public Object get(String key) {
                return map.get(key);
            }
        };
    }

}
