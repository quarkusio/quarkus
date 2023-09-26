package io.quarkus.smallrye.openapi.runtime;

import java.util.List;

import org.eclipse.microprofile.openapi.OASFilter;

public class OASFilters {

    private List<OASFilter> filters;

    public OASFilters(List<OASFilter> filters) {
        this.filters = filters;
    }

    public List<OASFilter> getFilters() {
        return this.filters;
    }
}
