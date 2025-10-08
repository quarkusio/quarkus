package io.quarkus.smallrye.openapi.runtime;

import java.util.List;

import jakarta.enterprise.event.Event;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.openapi.runtime.io.Format;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that serve the OpenAPI document in either json or yaml format
 */
public class OpenApiHandler implements Handler<RoutingContext> {

    private volatile OpenApiDocumentService openApiDocumentService;
    private static final String ALLOWED_METHODS = "GET, HEAD, OPTIONS";
    private static final String QUERY_PARAM_FORMAT = "format";

    private final String documentName;
    private final boolean alwaysRunFilter;
    private final Event<SecurityIdentity> securityIdentityEvent;
    private final CurrentIdentityAssociation currentIdentityAssociation;
    private final CurrentVertxRequest currentVertxRequest;
    private final ManagedContext requestContext;

    public OpenApiHandler(String documentName, boolean alwaysRunFilter) {
        this.documentName = documentName;
        this.alwaysRunFilter = alwaysRunFilter;

        if (alwaysRunFilter) {
            this.securityIdentityEvent = Arc.container().beanManager().getEvent().select(SecurityIdentity.class);
            this.currentVertxRequest = Arc.container().instance(CurrentVertxRequest.class).get();
            this.requestContext = Arc.container().requestContext();
            this.currentIdentityAssociation = Arc.container().instance(CurrentIdentityAssociation.class).get();
        } else {
            this.securityIdentityEvent = null;
            this.currentVertxRequest = null;
            this.requestContext = null;
            this.currentIdentityAssociation = null;
        }
    }

    @Override
    public void handle(RoutingContext context) {
        boolean manageRequestContext = alwaysRunFilter && !requestContext.isActive();

        try {
            if (manageRequestContext) {
                requestContext.activate();
                currentVertxRequest.setCurrent(context);
            }
            if (alwaysRunFilter) {
                associateSecurityIdentity(context);
            }
            invoke(context);
        } finally {
            if (manageRequestContext) {
                // Deactivate the context, i.e. cleanup the thread locals
                requestContext.deactivate();
            }
        }
    }

    private void associateSecurityIdentity(RoutingContext context) {
        QuarkusHttpUser user = (QuarkusHttpUser) context.user();

        if (currentIdentityAssociation != null) {
            if (user != null) {
                SecurityIdentity identity = user.getSecurityIdentity();
                currentIdentityAssociation.setIdentity(identity);
            } else {
                currentIdentityAssociation.setIdentity(QuarkusHttpUser.getSecurityIdentity(context, null));
            }
        }
        if (user != null) {
            securityIdentityEvent.fire(user.getSecurityIdentity());
        }
    }

    private void invoke(RoutingContext event) {
        HttpServerRequest req = event.request();
        HttpServerResponse resp = event.response();

        if (req.method().equals(HttpMethod.OPTIONS)) {
            resp.headers().set("Allow", ALLOWED_METHODS);
            event.next();
        } else {

            // Default content type is YAML
            Format format = Format.YAML;

            String path = event.normalizedPath();
            // Content negotiation with file extension
            if (path.endsWith(".json")) {
                format = Format.JSON;
            } else if (path.endsWith(".yaml") || path.endsWith(".yml")) {
                format = Format.YAML;
            } else {
                // Content negotiation with Accept header
                String accept = req.headers().get("Accept");

                List<String> formatParams = event.queryParam(QUERY_PARAM_FORMAT);
                String formatParam = formatParams.isEmpty() ? null : formatParams.get(0);

                // Check Accept, then query parameter "format" for JSON; else use YAML.
                if ((accept != null && accept.contains(Format.JSON.getMimeType())) ||
                        ("JSON".equalsIgnoreCase(formatParam))) {
                    format = Format.JSON;
                }
            }

            resp.headers().set("Content-Type", format.getMimeType() + ";charset=UTF-8");
            byte[] schemaDocument = getOpenApiDocumentService().getDocument(documentName, format);
            resp.end(Buffer.buffer(schemaDocument));
        }
    }

    private OpenApiDocumentService getOpenApiDocumentService() {
        if (this.openApiDocumentService == null) {
            this.openApiDocumentService = Arc.container().instance(OpenApiDocumentService.class).get();
        }
        return this.openApiDocumentService;
    }
}
