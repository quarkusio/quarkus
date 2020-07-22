package io.quarkus.qrs.runtime.model;

import java.util.ArrayList;
import java.util.List;

public class ResourceInterceptors {

    private List<ResourceRequestInterceptor> resourceRequestInterceptors = new ArrayList<>();

    public List<ResourceRequestInterceptor> getRequestInterceptors() {
        return resourceRequestInterceptors;
    }

    public void addRequestInterceptor(ResourceRequestInterceptor resourceRequestInterceptor) {
        this.resourceRequestInterceptors.add(resourceRequestInterceptor);
    }

}
