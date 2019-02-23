package io.quarkus.undertow.devmode;

import java.util.OptionalInt;

import javax.servlet.ServletException;

import io.quarkus.deployment.QuarkusConfig;
import io.quarkus.deployment.devmode.HotReplacementContext;
import io.quarkus.deployment.devmode.HotReplacementSetup;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.undertow.runtime.HttpConfig;
import io.quarkus.undertow.runtime.UndertowDeploymentTemplate;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class UndertowHotReplacementSetup implements HotReplacementSetup {

    private volatile long nextUpdate;
    private HotReplacementContext context;

    private static final long TWO_SECONDS = 2000;

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        this.context = context;
        HandlerWrapper wrapper = createHandlerWrapper();
        //TODO: we need to get these values from the config in runtime mode
        HttpConfig config = new HttpConfig();
        config.port = QuarkusConfig.getInt("quarkus.http.port", "8080");
        config.host = QuarkusConfig.getString("quarkus.http.host", "localhost", true);
        config.ioThreads = OptionalInt.empty();
        config.workerThreads = OptionalInt.empty();

        try {
            UndertowDeploymentTemplate.startUndertowEagerly(config, wrapper, LaunchMode.DEVELOPMENT);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
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

        if (nextUpdate > System.currentTimeMillis()) {
            if (context.getDeploymentProblem() != null) {
                ReplacementDebugPage.handleRequest(exchange, context.getDeploymentProblem());
                return;
            }
            next.handleRequest(exchange);
            return;
        }
        synchronized (this) {
            if (nextUpdate < System.currentTimeMillis()) {
                context.doScan();
                //we update at most once every 2s
                nextUpdate = System.currentTimeMillis() + TWO_SECONDS;

            }
        }
        if (context.getDeploymentProblem() != null) {
            ReplacementDebugPage.handleRequest(exchange, context.getDeploymentProblem());
            return;
        }
        next.handleRequest(exchange);
    }
}
