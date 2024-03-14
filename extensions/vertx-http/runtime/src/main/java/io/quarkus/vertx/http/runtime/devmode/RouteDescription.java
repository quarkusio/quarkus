package io.quarkus.vertx.http.runtime.devmode;

import java.util.ArrayList;
import java.util.List;

public class RouteDescription {

    private String basePath;
    private List<RouteMethodDescription> calls = new ArrayList<>();

    public RouteDescription() {
    }

    public RouteDescription(String basePath) {
        this.basePath = basePath;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public List<RouteMethodDescription> getCalls() {
        return calls;
    }

    public void addCall(RouteMethodDescription call) {
        this.calls.add(call);
    }
}
