package io.quarkus.qute.runtime.cache;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import jakarta.enterprise.event.Observes;

import io.quarkus.qute.CacheSectionHelper;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.ResultNode;

public class UnsupportedRemoteCacheConfigurator {

    void configureEngine(@Observes EngineBuilder builder) {
        builder.addSectionHelper(new CacheSectionHelper.Factory(new CacheSectionHelper.Cache() {

            @Override
            public CompletionStage<ResultNode> getValue(String key,
                    Function<String, CompletionStage<ResultNode>> loader) {
                throw new IllegalStateException("#cache is not supported for remote caches");
            }
        }));
    }

}
