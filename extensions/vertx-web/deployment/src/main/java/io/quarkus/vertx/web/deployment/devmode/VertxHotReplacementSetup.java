package io.quarkus.vertx.web.deployment.devmode;

import io.quarkus.deployment.devmode.HotReplacementContext;
import io.quarkus.deployment.devmode.HotReplacementSetup;
import io.quarkus.deployment.devmode.ReplacementDebugPage;
import io.quarkus.vertx.web.runtime.VertxWebTemplate;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class VertxHotReplacementSetup implements HotReplacementSetup {

    private volatile long nextUpdate;
    private HotReplacementContext hotReplacementContext;

    private static final long HOT_REPLACEMENT_INTERVAL = 2000;

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        this.hotReplacementContext = context;
        VertxWebTemplate.setHotReplacement(this::handleHotReplacementRequest);
    }

    void handleHotReplacementRequest(RoutingContext routingContext) {

        if (nextUpdate > System.currentTimeMillis()) {
            if (hotReplacementContext.getDeploymentProblem() != null) {
                handleDeploymentProblem(routingContext, hotReplacementContext.getDeploymentProblem());
                return;
            }
            routingContext.next();
            return;
        }
        boolean restart = false;
        synchronized (this) {
            if (nextUpdate < System.currentTimeMillis()) {
                try {
                    restart = hotReplacementContext.doScan();
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to perform hot replacement scanning", e);
                }
                nextUpdate = System.currentTimeMillis() + HOT_REPLACEMENT_INTERVAL;
            }
        }
        if (hotReplacementContext.getDeploymentProblem() != null) {
            handleDeploymentProblem(routingContext, hotReplacementContext.getDeploymentProblem());
            return;
        }
        if (restart) {
            routingContext.reroute(routingContext.request().path());
        } else {
            routingContext.next();
        }
    }

    public static void handleDeploymentProblem(RoutingContext routingContext, final Throwable exception) {
        String bodyText = ReplacementDebugPage.generateHtml(exception);
        HttpServerResponse response = routingContext.response();
        response.setStatusCode(500);
        response.headers().add("Content-Type", "text/html; charset=UTF-8");
        response.end(bodyText);
    }

}
