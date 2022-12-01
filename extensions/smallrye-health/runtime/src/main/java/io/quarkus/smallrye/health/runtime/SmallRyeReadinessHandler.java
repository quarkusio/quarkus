package io.quarkus.smallrye.health.runtime;

import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;
import io.vertx.ext.web.RoutingContext;

public class SmallRyeReadinessHandler extends SmallRyeHealthHandlerBase {

    @Override
    protected SmallRyeHealth getHealth(SmallRyeHealthReporter reporter, RoutingContext routingContext) {
        return reporter.getReadiness();
    }
}
