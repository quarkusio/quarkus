package io.quarkus.qute.runtime.cache;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import jakarta.enterprise.event.Observes;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.qute.CacheSectionHelper;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.ResultNode;
import io.quarkus.qute.cache.QuteCache;
import io.smallrye.mutiny.Uni;

public class CacheConfigurator {

    @CacheName(QuteCache.NAME)
    Cache cache;

    void configureEngine(@Observes EngineBuilder builder) {
        builder.addSectionHelper(new CacheSectionHelper.Factory(new CacheSectionHelper.Cache() {

            @Override
            public CompletionStage<ResultNode> getValue(String key,
                    Function<String, CompletionStage<ResultNode>> loader) {
                return cache.<String, ResultNode> getAsync(key, k -> Uni.createFrom().completionStage(loader.apply(k)))
                        .subscribeAsCompletionStage();
            }
        }));
    }

}
