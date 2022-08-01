package org.jboss.resteasy.reactive.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class PreMatchInterceptorContainer<T> extends InterceptorContainer<T> {
    private final List<ResourceInterceptor<T>> preMatchInterceptors = new ArrayList<>();

    public void addPreMatchInterceptor(ResourceInterceptor<T> interceptor) {
        preMatchInterceptors.add(interceptor);
    }

    public List<ResourceInterceptor<T>> getPreMatchInterceptors() {
        return preMatchInterceptors;
    }

    public void initializeDefaultFactories(Function<String, BeanFactory<?>> factoryCreator) {
        super.initializeDefaultFactories(factoryCreator);
        for (ResourceInterceptor<T> i : preMatchInterceptors) {
            if (i.getFactory() == null) {
                i.setFactory((BeanFactory<T>) factoryCreator.apply(i.getClassName()));
            }
        }
    }

    @Override
    public void sort() {
        super.sort();
        Collections.sort(preMatchInterceptors);
    }

    /**
     * Validates that any {@code ContainerRequestFilter} that has {@code nonBlockingRequired} set, comes before any other filter
     */
    public void validateThreadModel() {
        boolean unsetNonBlockingInterceptorFound = false;
        List<ResourceInterceptor<T>> allNonNamedInterceptors = new ArrayList<>(
                preMatchInterceptors.size() + getGlobalResourceInterceptors().size());
        allNonNamedInterceptors.addAll(preMatchInterceptors);
        allNonNamedInterceptors.addAll(getGlobalResourceInterceptors());
        for (ResourceInterceptor<T> filter : allNonNamedInterceptors) {
            if (filter.isNonBlockingRequired()) {
                if (unsetNonBlockingInterceptorFound) {
                    throw new RuntimeException(
                            "ContainerRequestFilters that are marked as '@NonBlocking' must be executed before any other filters. Offending class is '"
                                    + filter.getClassName() + "'");
                }
            } else {
                unsetNonBlockingInterceptorFound = true;
            }
        }
    }
}
