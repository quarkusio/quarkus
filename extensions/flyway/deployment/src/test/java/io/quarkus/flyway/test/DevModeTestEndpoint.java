package io.quarkus.flyway.test;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.flywaydb.core.Flyway;

@Path("/fly")
public class DevModeTestEndpoint {

    @Inject
    Instance<Flyway> flyway;

    @GET
    public boolean present() {
        flyway.get();
        return true;
    }

}
