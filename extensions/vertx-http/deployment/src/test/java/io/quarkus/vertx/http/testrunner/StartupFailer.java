package io.quarkus.vertx.http.testrunner;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.runtime.LaunchMode;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class StartupFailer {

    public void route(@Observes Router router) {
        //fail();
    }

    void fail() {
        if (LaunchMode.current() == LaunchMode.TEST) {
            throw new RuntimeException("FAIL");
        }
    }
}
