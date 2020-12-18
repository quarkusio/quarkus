package io.quarkus.qute;

import java.util.concurrent.CompletableFuture;
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
        return CompletableFuture.completedFuture(get(key));
    }

}
