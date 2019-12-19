package io.quarkus.vertx.web.runtime;

import io.vertx.core.http.HttpMethod;

public class RouteMatcher {

    private String path;
    private String regex;
    private String[] produces;
    private String[] consumes;
    private HttpMethod[] methods;
    private int order;

    public RouteMatcher() {
    }

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

    public void setPath(String path) {
        this.path = path;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String[] getProduces() {
        return produces;
    }

    public void setProduces(String[] produces) {
        this.produces = produces;
    }

    public String[] getConsumes() {
        return consumes;
    }

    public void setConsumes(String[] consumes) {
        this.consumes = consumes;
    }

    public HttpMethod[] getMethods() {
        return methods;
    }

    public void setMethods(HttpMethod[] methods) {
        this.methods = methods;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

}
