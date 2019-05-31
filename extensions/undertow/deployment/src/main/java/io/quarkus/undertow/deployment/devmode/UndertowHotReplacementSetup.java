package io.quarkus.undertow.deployment.devmode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.deployment.devmode.HotReplacementContext;
import io.quarkus.deployment.devmode.HotReplacementSetup;
import io.quarkus.deployment.devmode.ReplacementDebugPage;
import io.quarkus.undertow.runtime.UndertowDeploymentTemplate;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class UndertowHotReplacementSetup implements HotReplacementSetup {

    protected static final String META_INF_SERVICES = "META-INF/resources";
    private volatile long nextUpdate;
    private HotReplacementContext context;

    private static final long TWO_SECONDS = 2000;

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        this.context = context;
        HandlerWrapper wrapper = createHandlerWrapper();
        UndertowDeploymentTemplate.addHotDeploymentWrapper(wrapper);
        List<Path> resources = new ArrayList<>();
        for (Path i : context.getResourcesDir()) {
            Path resolved = i.resolve(META_INF_SERVICES);
            if (Files.exists(resolved)) {
                resources.add(resolved);
            }
        }
        UndertowDeploymentTemplate.setHotDeploymentResources(resources);
    }

    @Override
    public void handleFailedInitialStart() {
        UndertowDeploymentTemplate.startServerAfterFailedStart();
    }

    private HandlerWrapper createHandlerWrapper() {
        return new HandlerWrapper() {
            @Override
            public HttpHandler wrap(HttpHandler handler) {
                return new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        if (exchange.isInIoThread()) {
                            exchange.dispatch(this);
                            return;
                        }
                        handleHotDeploymentRequest(exchange, handler);
                    }
                };
            }
        };
    }

    void handleHotDeploymentRequest(HttpServerExchange exchange, HttpHandler next) throws Exception {
        if (nextUpdate < System.currentTimeMillis()) {
            synchronized (this) {
                if (nextUpdate < System.currentTimeMillis()) {
                    context.doScan(true);
                    // we update at most once every 2s
                    nextUpdate = System.currentTimeMillis() + TWO_SECONDS;
                }
            }
        }
        if (context.getDeploymentProblem() != null) {
            handleDeploymentProblem(exchange, context.getDeploymentProblem());
            return;
        }
        next.handleRequest(exchange);
    }

    private void handleDeploymentProblem(HttpServerExchange exchange, final Throwable exception) {
        String bodyText = ReplacementDebugPage.generateHtml(exception);
        exchange.setStatusCode(500);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html; charset=UTF-8");
        exchange.getResponseSender().send(bodyText);
    }
}
