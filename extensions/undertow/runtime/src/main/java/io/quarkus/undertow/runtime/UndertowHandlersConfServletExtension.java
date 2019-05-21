package io.quarkus.undertow.runtime;

import java.io.InputStream;
import java.util.List;

import javax.servlet.ServletContext;

import io.undertow.Handlers;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.builder.PredicatedHandler;
import io.undertow.server.handlers.builder.PredicatedHandlersParser;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;

/**
 * Registers the Undertow handlers configured in the app's META-INF/undertow-handlers.conf
 */
public class UndertowHandlersConfServletExtension implements ServletExtension {

    private static final String META_INF_UNDERTOW_HANDLERS_CONF = "META-INF/undertow-handlers.conf";

    public static boolean existsConfFile() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return classLoader.getResource(META_INF_UNDERTOW_HANDLERS_CONF) != null;
    }

    @Override
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream handlers = classLoader.getResourceAsStream(META_INF_UNDERTOW_HANDLERS_CONF);
        if (handlers != null) {
            // From Stuart Douglas: Ideally these would be parsed at deployment time and passed into a template,
            // however they are likely not bytecode serialisable. Even though this approach
            // does not 100% align with the Quarkus ethos I think it is ok in this case as
            // the gains would be marginal compared to the cost of attempting to make
            // every predicate bytecode serialisable.
            List<PredicatedHandler> handlerList = PredicatedHandlersParser.parse(handlers, classLoader);
            if (!handlerList.isEmpty()) {
                deploymentInfo.addOuterHandlerChainWrapper(new RewriteCorrectingHandlerWrappers.PostWrapper());
                deploymentInfo.addOuterHandlerChainWrapper(new HandlerWrapper() {
                    @Override
                    public HttpHandler wrap(HttpHandler handler) {
                        return Handlers.predicates(handlerList, handler);
                    }
                });
                deploymentInfo.addOuterHandlerChainWrapper(new RewriteCorrectingHandlerWrappers.PreWrapper());
            }
        }
    }
}
