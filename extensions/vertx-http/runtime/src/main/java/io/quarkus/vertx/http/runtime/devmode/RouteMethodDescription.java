package io.quarkus.vertx.http.runtime.devmode;

public class RouteMethodDescription {
    private String javaMethod;
    private String httpMethod;
    private String fullPath;
    private String produces;
    private String consumes;

    public RouteMethodDescription() {
        super();
    }

    public RouteMethodDescription(String httpMethod, String fullPath, String produces, String consumes) {
        super();
        this.javaMethod = null;
        this.httpMethod = httpMethod;
        this.fullPath = fullPath;
        this.produces = produces;
        this.consumes = consumes;
    }

    public RouteMethodDescription(String javaMethod, String httpMethod, String fullPath, String produces, String consumes) {
        super();
        this.javaMethod = javaMethod;
        this.httpMethod = httpMethod;
        this.fullPath = fullPath;
        this.produces = produces;
        this.consumes = consumes;
    }

    public String getJavaMethod() {
        return javaMethod;
    }

    public void setJavaMethod(String javaMethod) {
        this.javaMethod = javaMethod;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public String getProduces() {
        return produces;
    }

    public void setProduces(String produces) {
        this.produces = produces;
    }

    public String getConsumes() {
        return consumes;
    }

    public void setConsumes(String consumes) {
        this.consumes = consumes;
    }

    @Override
    public String toString() {
        return "RouteMethodDescription{" + "javaMethod=" + javaMethod + ", httpMethod=" + httpMethod + ", fullPath=" + fullPath
                + ", produces=" + produces + ", consumes=" + consumes + '}';
    }
}
