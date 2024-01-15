package io.quarkus.restclient.deployment;

import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Register provider against every Rest client matching predicate.
 */
public final class RestClientPredicateProviderBuildItem extends MultiBuildItem {

    private final String providerClass;
    private final Predicate<ClassInfo> matcher;

    /**
     * Register JAX-RS client provider against Rest clients matching {@code matcher} condition.
     */
    public RestClientPredicateProviderBuildItem(String providerClass, Predicate<ClassInfo> matcher) {
        this.providerClass = providerClass;
        this.matcher = matcher;
    }

    public String getProviderClass() {
        return providerClass;
    }

    /**
     * Test whether the {@link #providerClass} should be added to {@code restClientClassInfo} as provider.
     */
    boolean appliesTo(ClassInfo restClientClassInfo) {
        return matcher.test(restClientClassInfo);
    }

}
