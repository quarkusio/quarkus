package io.quarkus.vertx.http.runtime;

import io.quarkus.vertx.http.ManagementInterface;
import io.vertx.ext.web.Router;

public class ManagementInterfaceImpl implements ManagementInterface {

    private final Router router;

    public ManagementInterfaceImpl(Router router) {
        this.router = router;
    }

    @Override
    public Router router() {
        return router;
    }
}
