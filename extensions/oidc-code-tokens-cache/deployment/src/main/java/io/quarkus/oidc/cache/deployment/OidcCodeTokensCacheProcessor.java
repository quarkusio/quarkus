package io.quarkus.oidc.cache.deployment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.cache.deployment.AdditionalCacheNameBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.oidc.runtime.CacheTokenStateManager;

class OidcCodeTokensCacheProcessor {

    private static final String FEATURE = "oidc-code-tokens-cache";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem cacheTokenStateManager(Capabilities capabilities) {

        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();

        builder.addBeanClass(CacheTokenStateManager.class);

        return builder.build();
    }

    @BuildStep
    List<AdditionalCacheNameBuildItem> cacheNames(CombinedIndexBuildItem combinedIndex) {
        Set<String> cacheNames = new HashSet<>();
        cacheNames.add("tokenState");
        List<AdditionalCacheNameBuildItem> result = new ArrayList<>(cacheNames.size());
        for (String cacheName : cacheNames) {
            result.add(new AdditionalCacheNameBuildItem(cacheName));
        }
        return result;
    }

}
