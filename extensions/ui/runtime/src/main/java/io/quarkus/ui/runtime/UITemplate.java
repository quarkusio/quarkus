package io.quarkus.ui.runtime;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletContext;

import io.quarkus.runtime.annotations.Template;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.handlers.ServletRequestContext;

@Template
public class UITemplate {

    public ServletExtension addUiHandler(int port) {
        return new ServletExtension() {
            @Override
            public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {

                deploymentInfo.addServlet(new ServletInfo("default", UIDefaultServlet.class));

                deploymentInfo.addOuterHandlerChainWrapper(new HandlerWrapper() {
                    @Override
                    public HttpHandler wrap(HttpHandler handler) {
                        try {
                            ProxyHandler proxyHandler = ProxyHandler.builder()
                                    .setProxyClient(new LoadBalancingProxyClient()
                                            .addHost(new URI("http://localhost:" + port)))
                                    .build();

                            return new HttpHandler() {
                                @Override
                                public void handleRequest(HttpServerExchange exchange) throws Exception {
                                    exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY).getOriginalRequest()
                                            .setAttribute("io.quarkus.ui-proxy", true);
                                    handler.handleRequest(exchange);
                                    if (exchange.getAttachment(UIDefaultServlet.DEFAULT_REQUEST) != null) {
                                        proxyHandler.handleRequest(exchange);
                                    }
                                }
                            };
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        };
    }

}
