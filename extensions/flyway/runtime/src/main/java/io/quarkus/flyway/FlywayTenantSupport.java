package io.quarkus.flyway;

import java.util.List;

public interface FlywayTenantSupport {

    List<String> getTenantsToInitialize();
}
