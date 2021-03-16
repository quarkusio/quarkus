package io.quarkus.vertx.web.runtime;

import io.quarkus.runtime.annotations.RecordableConstructor;
import io.vertx.core.http.HttpMethod;

public class RouteMatcher {

    private final String path;
    private final String regex;
    private final String[] produces;
    private final String[] consumes;
    private final HttpMethod[] methods;
    private final int order;

    @RecordableConstructor
    public RouteMatcher(String path, String regex, String[] produces, String[] consumes, HttpMethod[] methods, int order) {
        this.path = path;
        this.regex = regex;
        this.produces = produces;
        this.consumes = consumes;
        this.methods = methods;
        this.order = order;
    }

    public String getPath() {
        return path;
    }

    public String getRegex() {
        return regex;
    }

    public String[] getProduces() {
        return produces;
    }

    public String[] getConsumes() {
        return consumes;
    }

    public HttpMethod[] getMethods() {
        return methods;
    }

    public int getOrder() {
        return order;
    }

}
