package io.quarkus.security.jpa.reactive;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationScoped
@ApplicationPath("/jaxrs-secured")
public class TestApplication extends Application {
    // intentionally left empty
}
