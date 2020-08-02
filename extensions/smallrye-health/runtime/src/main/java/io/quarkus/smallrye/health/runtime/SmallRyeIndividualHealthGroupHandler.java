package io.quarkus.smallrye.health.runtime;

import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;
import io.vertx.ext.web.RoutingContext;

public class SmallRyeIndividualHealthGroupHandler extends SmallRyeHealthHandlerBase {

    @Override
    protected SmallRyeHealth getHealth(SmallRyeHealthReporter reporter, RoutingContext ctx) {
        String group = ctx.normalisedPath().substring(ctx.normalisedPath().lastIndexOf("/") + 1);
        return reporter.getHealthGroup(group);
    }
}
