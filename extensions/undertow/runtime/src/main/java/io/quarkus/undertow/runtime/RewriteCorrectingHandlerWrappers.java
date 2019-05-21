package io.quarkus.undertow.runtime;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletPathMatch;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.AttachmentKey;

/**
 * Handler that works around issues with rewrites() and undertow-handlers.conf.
 * <p>
 * Because the rewrite happens after the initial dispatch this handler detects if
 * the path has been rewritten and updates the servlet target.
 *
 * This is a bit of a hack, it needs a lot more thinking about a clean way to handle
 * this
 */
public class RewriteCorrectingHandlerWrappers {

    private static final AttachmentKey<String> OLD_RELATIVE_PATH = AttachmentKey.create(String.class);

    static class PreWrapper implements HandlerWrapper {

        @Override
        public HttpHandler wrap(final HttpHandler handler) {
            return new HttpHandler() {
                @Override
                public void handleRequest(HttpServerExchange exchange) throws Exception {
                    exchange.putAttachment(OLD_RELATIVE_PATH, exchange.getRelativePath());
                    handler.handleRequest(exchange);
                }
            };
        }
    }

    static class PostWrapper implements HandlerWrapper {
        @Override
        public HttpHandler wrap(final HttpHandler handler) {
            return new HttpHandler() {
                @Override
                public void handleRequest(HttpServerExchange exchange) throws Exception {
                    String old = exchange.getAttachment(OLD_RELATIVE_PATH);
                    if (!old.equals(exchange.getRelativePath())) {
                        ServletRequestContext src = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
                        ServletPathMatch info = src.getDeployment().getServletPaths()
                                .getServletHandlerByPath(exchange.getRelativePath());
                        src.setCurrentServlet(info.getServletChain());
                        src.setServletPathMatch(info);
                    }
                    handler.handleRequest(exchange);
                }
            };
        }
    }

}
