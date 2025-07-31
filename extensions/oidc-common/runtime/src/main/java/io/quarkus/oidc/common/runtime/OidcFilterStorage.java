package io.quarkus.oidc.common.runtime;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.OidcRequestFilter.OidcRequestContext;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.quarkus.oidc.common.OidcResponseFilter.OidcResponseContext;

public abstract class OidcFilterStorage {

    public interface OidcRequestContextPredicate extends Predicate<OidcRequestContext> {
    }

    public interface OidcResponseContextPredicate extends Predicate<OidcResponseContext> {
    }

    private record OidcRequestFilterItem(OidcRequestFilter filter, Predicate<OidcRequestContext> filterCondition) {
    }

    private record OidcResponseFilterItem(OidcResponseFilter filter, Predicate<OidcResponseContext> filterCondition) {
    }

    private final Map<OidcEndpoint.Type, List<OidcRequestFilterItem>> conditionalRequestFilters;
    private final Map<OidcEndpoint.Type, List<OidcResponseFilterItem>> conditionalResponseFilters;
    private final Map<OidcEndpoint.Type, List<OidcRequestFilter>> requestFilters;
    private final Map<OidcEndpoint.Type, List<OidcResponseFilter>> responseFilters;
    private final boolean isEmpty;

    public OidcFilterStorage(InjectableInstance<OidcRequestFilter> allRequestFilters,
            InjectableInstance<OidcResponseFilter> allResponseFilters) {
        var builder = createBuilder(allRequestFilters, allResponseFilters);
        this.conditionalRequestFilters = builder.getConditionalRequestFilters();
        this.conditionalResponseFilters = builder.getConditionalResponseFilters();
        this.requestFilters = builder.requestFilters;
        this.responseFilters = builder.responseFilters;
        this.isEmpty = requestFilters.isEmpty() && responseFilters.isEmpty() && conditionalRequestFilters == null
                && conditionalResponseFilters == null;
    }

    public final boolean isEmpty() {
        return isEmpty;
    }

    public final List<OidcRequestFilter> getOidcRequestFilters(OidcEndpoint.Type type) {
        if (isEmpty) {
            return List.of();
        }
        return requestFilters.getOrDefault(Objects.requireNonNull(type), List.of());
    }

    public final List<OidcResponseFilter> getOidcResponseFilters(OidcEndpoint.Type type) {
        if (isEmpty) {
            return List.of();
        }
        return responseFilters.getOrDefault(Objects.requireNonNull(type), List.of());
    }

    public final List<OidcRequestFilter> getOidcRequestFilters(OidcEndpoint.Type type, OidcRequestContext context) {
        if (isEmpty) {
            return List.of();
        }
        if (context == null || conditionalRequestFilters == null || !conditionalRequestFilters.containsKey(type)) {
            return getOidcRequestFilters(type);
        }

        // there is at least one conditional request filter
        return Stream.concat(getOidcRequestFilters(type).stream(), conditionalRequestFilters.get(type).stream()
                .filter(i -> i.filterCondition.test(context)).map(OidcRequestFilterItem::filter)).toList();
    }

    public final List<OidcResponseFilter> getOidcResponseFilters(OidcEndpoint.Type type, OidcResponseContext context) {
        if (isEmpty) {
            return List.of();
        }
        if (context == null || conditionalResponseFilters == null || !conditionalResponseFilters.containsKey(type)) {
            return getOidcResponseFilters(type);
        }

        // there is at least one conditional response filter
        return Stream.concat(getOidcResponseFilters(type).stream(), conditionalResponseFilters.get(type).stream()
                .filter(i -> i.filterCondition.test(context)).map(OidcResponseFilterItem::filter)).toList();
    }

    public static OidcFilterStorage get() {
        return Arc.container().select(OidcFilterStorage.class).get();
    }

    private OidcFilterStorageBuilder createBuilder(InjectableInstance<OidcRequestFilter> allRequestFilters,
            InjectableInstance<OidcResponseFilter> allResponseFilters) {
        final var builder = new OidcFilterStorageBuilder();
        for (InstanceHandle<OidcRequestFilter> handle : allRequestFilters.handles()) {
            final OidcRequestFilter requestFilter = handle.get();
            final Class<?> requestFilterClass = getBeanClass(handle, requestFilter);
            List<OidcEndpoint.Type> oidcEndpointTypes = getRequestFilterEndpointTypes(requestFilterClass);
            if (oidcEndpointTypes.isEmpty()) {
                // this should be impossible as we put ALL as the default type for unannotated filters
                throw new IllegalStateException(
                        "No OidcEndpoint.Type found for request filter: " + requestFilterClass.getName());
            }
            List<OidcRequestContextPredicate> predicates = getRequestFilterPredicates(requestFilterClass);
            if (predicates == null || predicates.isEmpty()) {
                builder.addRequestFilter(oidcEndpointTypes, requestFilter);
            } else {
                builder.addConditionalRequestFilter(oidcEndpointTypes, requestFilter, predicates);
            }
        }
        for (InstanceHandle<OidcResponseFilter> handle : allResponseFilters.handles()) {
            final OidcResponseFilter responseFilter = handle.get();
            final Class<?> responseFilterClass = getBeanClass(handle, responseFilter);
            List<OidcEndpoint.Type> oidcEndpointTypes = getResponseFilterEndpointTypes(responseFilterClass);
            if (oidcEndpointTypes.isEmpty()) {
                // this should be impossible as we put ALL as the default type for unannotated filters
                throw new IllegalStateException(
                        "No OidcEndpoint.Type found for response filter: " + responseFilterClass.getName());
            }
            List<OidcResponseContextPredicate> predicates = getResponseFilterPredicates(responseFilterClass);
            if (predicates == null || predicates.isEmpty()) {
                builder.addResponseFilter(oidcEndpointTypes, responseFilter);
            } else {
                builder.addConditionalResponseFilter(oidcEndpointTypes, responseFilter, predicates);
            }
        }
        return builder.build();
    }

    protected abstract List<OidcEndpoint.Type> getResponseFilterEndpointTypes(Class<?> filterClass);

    protected abstract List<OidcEndpoint.Type> getRequestFilterEndpointTypes(Class<?> filterClass);

    protected abstract List<OidcRequestContextPredicate> getRequestFilterPredicates(Class<?> filterClass);

    protected abstract List<OidcResponseContextPredicate> getResponseFilterPredicates(Class<?> filterClass);

    private static <T> Class<?> getBeanClass(InstanceHandle<T> handle, T filter) {
        var bean = handle.getBean();
        if (bean.getKind() == InjectableBean.Kind.CLASS) {
            return bean.getImplementationClass();
        }
        T implementation = ClientProxy.unwrap(filter);
        return implementation.getClass();
    }

    private record OidcFilterStorageBuilder(Map<OidcEndpoint.Type, List<OidcRequestFilterItem>> conditionalRequestFilters,
            Map<OidcEndpoint.Type, List<OidcResponseFilterItem>> conditionalResponseFilters,
            Map<OidcEndpoint.Type, List<OidcRequestFilter>> requestFilters,
            Map<OidcEndpoint.Type, List<OidcResponseFilter>> responseFilters) {

        private OidcFilterStorageBuilder() {
            this(new EnumMap<>(OidcEndpoint.Type.class), new EnumMap<>(OidcEndpoint.Type.class),
                    new EnumMap<>(OidcEndpoint.Type.class), new EnumMap<>(OidcEndpoint.Type.class));
        }

        private void addRequestFilter(List<OidcEndpoint.Type> endpointTypes, OidcRequestFilter requestFilter) {
            Objects.requireNonNull(requestFilter);
            for (OidcEndpoint.Type endpointType : endpointTypes) {
                requestFilters.computeIfAbsent(endpointType, k -> new ArrayList<>()).add(requestFilter);
            }
        }

        private void addResponseFilter(List<OidcEndpoint.Type> endpointTypes, OidcResponseFilter responseFilter) {
            Objects.requireNonNull(responseFilter);
            for (OidcEndpoint.Type endpointType : endpointTypes) {
                responseFilters.computeIfAbsent(endpointType, k -> new ArrayList<>()).add(responseFilter);
            }
        }

        private void addConditionalRequestFilter(List<OidcEndpoint.Type> endpointTypes, OidcRequestFilter requestFilter,
                List<OidcRequestContextPredicate> filterConditions) {
            Objects.requireNonNull(requestFilter);
            Objects.requireNonNull(filterConditions);
            if (filterConditions.isEmpty()) {
                throw new IllegalArgumentException("Conditional request filter has no conditions");
            }
            Predicate<OidcRequestContext> filterCondition = filterConditions.stream()
                    .reduce((t, other) -> ctx -> t.test(ctx) && other.test(ctx)).get();
            for (OidcEndpoint.Type endpointType : endpointTypes) {
                conditionalRequestFilters.computeIfAbsent(endpointType, k -> new ArrayList<>())
                        .add(new OidcRequestFilterItem(requestFilter, filterCondition));
            }
        }

        private void addConditionalResponseFilter(List<OidcEndpoint.Type> endpointTypes, OidcResponseFilter responseFilter,
                List<OidcResponseContextPredicate> filterConditions) {
            Objects.requireNonNull(responseFilter);
            Objects.requireNonNull(filterConditions);
            if (filterConditions.isEmpty()) {
                throw new IllegalArgumentException("Conditional response filter has no conditions");
            }
            Predicate<OidcResponseContext> filterCondition = filterConditions.stream()
                    .reduce((t, other) -> ctx -> t.test(ctx) && other.test(ctx)).get();
            for (OidcEndpoint.Type endpointType : endpointTypes) {
                conditionalResponseFilters.computeIfAbsent(endpointType, k -> new ArrayList<>())
                        .add(new OidcResponseFilterItem(responseFilter, filterCondition));
            }
        }

        private Map<OidcEndpoint.Type, List<OidcRequestFilterItem>> getConditionalRequestFilters() {
            return conditionalRequestFilters.isEmpty() ? null : conditionalRequestFilters;
        }

        private Map<OidcEndpoint.Type, List<OidcResponseFilterItem>> getConditionalResponseFilters() {
            return conditionalResponseFilters.isEmpty() ? null : conditionalResponseFilters;
        }

        private OidcFilterStorageBuilder build() {
            addAllToOtherEndpointTypes(conditionalRequestFilters);
            addAllToOtherEndpointTypes(conditionalResponseFilters);
            addAllToOtherEndpointTypes(requestFilters);
            addAllToOtherEndpointTypes(responseFilters);
            makeListsImmutable(conditionalRequestFilters);
            makeListsImmutable(conditionalResponseFilters);
            makeListsImmutable(requestFilters);
            makeListsImmutable(responseFilters);
            return this;
        }

        private static <T> void makeListsImmutable(Map<OidcEndpoint.Type, List<T>> filters) {
            filters.replaceAll((t, filterList) -> List.copyOf(filterList));
        }

        private static <T> void addAllToOtherEndpointTypes(Map<OidcEndpoint.Type, List<T>> filters) {
            var filtersAppliedToAll = filters.get(OidcEndpoint.Type.ALL);
            if (filtersAppliedToAll != null && !filtersAppliedToAll.isEmpty()) {
                EnumSet.complementOf(EnumSet.of(OidcEndpoint.Type.ALL))
                        .forEach(type -> filters.computeIfAbsent(type, k -> new ArrayList<>()).addAll(filtersAppliedToAll));
            }
        }
    }
}
