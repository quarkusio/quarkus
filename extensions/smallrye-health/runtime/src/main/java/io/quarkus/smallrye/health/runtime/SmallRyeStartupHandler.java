package io.quarkus.smallrye.health.runtime;

import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class SmallRyeStartupHandler extends SmallRyeHealthHandlerBase {

    @Override
    protected Uni<SmallRyeHealth> getHealth(SmallRyeHealthReporter reporter, RoutingContext ctx) {
        return reporter.getStartupAsync();
    }
}
