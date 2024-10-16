package io.quarkus.vertx.http.devmode.management;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@Named
@ApplicationScoped
public class LiveReloadManagementBean {

    private final String value = "string1";

    String string() {
        return value;
    }

}
