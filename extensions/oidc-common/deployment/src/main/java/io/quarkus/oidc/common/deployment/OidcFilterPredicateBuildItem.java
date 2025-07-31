package io.quarkus.oidc.common.deployment;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.oidc.common.OidcRequestFilter;

/**
 * This build item allows OIDC, OIDC Client, or OIDC Client Registration extensions to register additional
 * {@link OidcRequestFilter} or {@link io.quarkus.oidc.common.OidcResponseFilter} condition
 * in addition to the {@link io.quarkus.oidc.common.OidcEndpoint.Type} condition.
 */
public final class OidcFilterPredicateBuildItem extends MultiBuildItem {

    private enum FilterType {
        REQUEST,
        RESPONSE
    }

    private final FilterType filterType;
    private final Predicate<ClassInfo> appliesToPredicate;
    final String predicateClass;

    private OidcFilterPredicateBuildItem(Predicate<ClassInfo> appliesToPredicate, String predicateClass,
            FilterType filterType) {
        this.appliesToPredicate = Objects.requireNonNull(appliesToPredicate);
        this.predicateClass = Objects.requireNonNull(predicateClass);
        this.filterType = filterType;
    }

    boolean appliesTo(ClassInfo filterClassInfo) {
        return appliesToPredicate.test(filterClassInfo);
    }

    public static OidcFilterPredicateBuildItem requestFilter(DotName annotationName, String predicateClass) {
        Objects.requireNonNull(annotationName);
        return new OidcFilterPredicateBuildItem(classInfo -> classInfo.hasAnnotation(annotationName), predicateClass,
                FilterType.REQUEST);
    }

    public static OidcFilterPredicateBuildItem responseFilter(DotName annotationName, String predicateClass) {
        Objects.requireNonNull(annotationName);
        return new OidcFilterPredicateBuildItem(classInfo -> classInfo.hasAnnotation(annotationName), predicateClass,
                FilterType.RESPONSE);
    }

    public static OidcFilterPredicateBuildItem requestFilter(Predicate<ClassInfo> appliesToPredicate, String predicateClass) {
        return new OidcFilterPredicateBuildItem(appliesToPredicate, predicateClass, FilterType.REQUEST);
    }

    public static OidcFilterPredicateBuildItem responseFilter(Predicate<ClassInfo> appliesToPredicate, String predicateClass) {
        return new OidcFilterPredicateBuildItem(appliesToPredicate, predicateClass, FilterType.RESPONSE);
    }

    static Collection<OidcFilterPredicateBuildItem> getRequestFilters(Collection<OidcFilterPredicateBuildItem> filters) {
        return getFilters(filters, FilterType.REQUEST);
    }

    static Collection<OidcFilterPredicateBuildItem> getResponseFilters(Collection<OidcFilterPredicateBuildItem> filters) {
        return getFilters(filters, FilterType.RESPONSE);
    }

    private static Collection<OidcFilterPredicateBuildItem> getFilters(Collection<OidcFilterPredicateBuildItem> filters,
            FilterType filterType) {
        return filters.stream().filter(f -> f.filterType == filterType).toList();
    }
}
