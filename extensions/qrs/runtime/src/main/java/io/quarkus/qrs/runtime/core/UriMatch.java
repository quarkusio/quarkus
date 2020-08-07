package io.quarkus.qrs.runtime.core;

import io.quarkus.qrs.runtime.mapping.RuntimeResource;
import io.quarkus.qrs.runtime.spi.BeanFactory;

public class UriMatch {
    public final String matched;
    public final RuntimeResource resource;
    public final BeanFactory.BeanInstance<?> target;

    public UriMatch(String matched, RuntimeResource resource, BeanFactory.BeanInstance<?> target) {
        this.matched = matched;
        this.resource = resource;
        this.target = target;
    }
}
