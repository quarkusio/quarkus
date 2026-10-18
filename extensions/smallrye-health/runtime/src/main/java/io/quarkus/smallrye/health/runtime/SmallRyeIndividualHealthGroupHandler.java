package io.quarkus.smallrye.health.runtime;

import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class SmallRyeIndividualHealthGroupHandler extends SmallRyeHealthHandlerBase {

    @Override
    protected Uni<SmallRyeHealth> getHealth(SmallRyeHealthReporter reporter, RoutingContext ctx) {
        String group = ctx.normalizedPath().substring(ctx.normalizedPath().lastIndexOf("/") + 1);
        return reporter.getHealthGroupAsync(group);
    }
}
