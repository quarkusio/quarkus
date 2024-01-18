package io.quarkus.jfr.runtime.http;

import jdk.jfr.Event;

public abstract class AbstractHttpEvent extends Event {

    protected String httpMethod;
    protected String uri;
    protected String resourceClass;
    protected String resourceMethod;
    protected String client;

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setResourceClass(String resourceClass) {
        this.resourceClass = resourceClass;
    }

    public void setResourceMethod(String resourceMethod) {
        this.resourceMethod = resourceMethod;
    }

    public void setClient(String client) {
        this.client = client;
    }
}
