package io.quarkus.security.jpa;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationScoped
@ApplicationPath("/jaxrs-secured")
public class TestApplication extends Application {
    // intentionally left empty
}
