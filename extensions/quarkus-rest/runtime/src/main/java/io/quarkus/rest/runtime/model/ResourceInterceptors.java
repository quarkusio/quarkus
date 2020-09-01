package io.quarkus.rest.runtime.model;

import java.util.ArrayList;
import java.util.List;

public class ResourceInterceptors {

    private List<ResourceRequestInterceptor> resourcePreMatchRequestInterceptors = new ArrayList<>();
    private List<ResourceRequestInterceptor> resourceRequestInterceptors = new ArrayList<>();
    private List<ResourceResponseInterceptor> resourceResponseInterceptors = new ArrayList<>();

    public List<ResourceRequestInterceptor> getRequestInterceptors() {
        return resourceRequestInterceptors;
    }

    public List<ResourceResponseInterceptor> getResponseInterceptors() {
        return resourceResponseInterceptors;
    }

    public List<ResourceRequestInterceptor> getResourcePreMatchRequestInterceptors() {
        return resourcePreMatchRequestInterceptors;
    }

    public void addRequestInterceptor(ResourceRequestInterceptor interceptor) {
        this.resourceRequestInterceptors.add(interceptor);
    }

    public void addResponseInterceptor(ResourceResponseInterceptor interceptor) {
        this.resourceResponseInterceptors.add(interceptor);
    }

    public void addResourcePreMatchInterceptor(ResourceRequestInterceptor interceptor) {
        resourcePreMatchRequestInterceptors.add(interceptor);
    }

}
