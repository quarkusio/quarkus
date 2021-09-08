package io.quarkus.oidc.deployment.devservices;

import java.time.Duration;

import org.jboss.logging.Logger;

import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

public class OidcAuthorizationCodePostHandler extends DevConsolePostHandler {
    private static final Logger LOG = Logger.getLogger(OidcAuthorizationCodePostHandler.class);

    Vertx vertxInstance;
    Duration timeout;

    public OidcAuthorizationCodePostHandler(Vertx vertxInstance, Duration timeout) {
        this.vertxInstance = vertxInstance;
        this.timeout = timeout;
    }

    @Override
    protected void handlePostAsync(RoutingContext event, MultiMap form) throws Exception {
        WebClient client = OidcDevServicesUtils.createWebClient(vertxInstance);
        String tokenUrl = form.get("tokenUrl");

        try {
            LOG.infof("Using authorization_code grant to get a token from '%s' with client id '%s'",
                    tokenUrl, form.get("client"));

            HttpRequest<Buffer> request = client.postAbs(tokenUrl);
            request.putHeader(HttpHeaders.CONTENT_TYPE.toString(), HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString());

            io.vertx.mutiny.core.MultiMap props = new io.vertx.mutiny.core.MultiMap(MultiMap.caseInsensitiveMultiMap());
            props.add("client_id", form.get("client"));
            if (form.get("clientSecret") != null && !form.get("clientSecret").isBlank()) {
                props.add("client_secret", form.get("clientSecret"));
            }
            props.add("grant_type", "authorization_code");
            props.add("code", form.get("authorizationCode"));
            props.add("redirect_uri", form.get("redirectUri"));

            String tokens = request.sendBuffer(OidcCommonUtils.encodeForm(props)).onItem()
                    .transform(resp -> getBodyAsString(resp))
                    .await().atMost(timeout);

            event.put("tokens", tokens);

        } catch (Throwable t) {
            LOG.errorf("Token can not be acquired from OpenId Connect provider: %s", t.toString());
        } finally {
            client.close();
        }
    }

    @Override
    protected void actionSuccess(RoutingContext event) {
        event.response().setStatusCode(200);
        String tokens = (String) event.get("tokens");
        if (tokens != null) {
            event.response().end(tokens);
        }
    }

    private static String getBodyAsString(HttpResponse<Buffer> resp) {
        if (resp.statusCode() == 200) {
            return resp.bodyAsString();
        } else {
            String errorMessage = resp.bodyAsString();
            throw new RuntimeException(errorMessage);
        }
    }
}
