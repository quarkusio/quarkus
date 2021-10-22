package io.quarkus.oidc.deployment.devservices;

import java.time.Duration;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.client.WebClient;

public class OidcPasswordClientCredHandler extends DevConsolePostHandler {
    private static final Logger LOG = Logger.getLogger(OidcPasswordClientCredHandler.class);
    Map<String, String> users;

    Vertx vertxInstance;
    Duration timeout;

    public OidcPasswordClientCredHandler(Vertx vertxInstance, Duration timeout) {
        this(vertxInstance, timeout, Map.of());
    }

    public OidcPasswordClientCredHandler(Vertx vertxInstance, Duration timeout, Map<String, String> users) {
        this.vertxInstance = vertxInstance;
        this.timeout = timeout;
        this.users = users;
    }

    @Override
    protected void handlePostAsync(RoutingContext event, MultiMap form) throws Exception {
        WebClient client = OidcDevServicesUtils.createWebClient(vertxInstance);
        String tokenUrl = form.get("tokenUrl");

        try {
            String token = null;
            if ("password".equals(form.get("grant"))) {
                LOG.infof("Using a password grant to get a token from '%s' for user '%s' with client id '%s'",
                        tokenUrl, form.get("user"), form.get("client"));

                String userName = form.get("user");
                String password = users.get(userName);
                if (password == null) {
                    password = form.get("password");
                }
                token = OidcDevServicesUtils.getPasswordAccessToken(client, tokenUrl,
                        form.get("client"), form.get("clientSecret"),
                        userName,
                        password,
                        timeout);
            } else {
                LOG.infof("Using a client_credentials grant to get a token token from '%s' with client id '%s'",
                        tokenUrl, form.get("client"));

                token = OidcDevServicesUtils.getClientCredAccessToken(client, tokenUrl,
                        form.get("client"),
                        form.get("clientSecret"),
                        timeout);
            }
            LOG.infof("Test token: %s", token);
            if (form.get("serviceUrl") != null) {
                testServiceInternal(event, client, form.get("serviceUrl"), token);
            } else {
                // only token is required
                event.put("result", token);
            }
        } catch (Throwable t) {
            LOG.errorf("Token can not be acquired from OpenId Connect provider: %s", t.toString());
        } finally {
            client.close();
        }
    }

    private void testServiceInternal(RoutingContext event, WebClient client, String serviceUrl, String token) {
        try {
            LOG.infof("Sending token to '%s'", serviceUrl);
            int statusCode = client.getAbs(serviceUrl)
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token).send().await().indefinitely()
                    .statusCode();
            LOG.infof("Result: %d", statusCode);
            event.put("result", String.valueOf(statusCode));
        } catch (Throwable t) {
            LOG.errorf("Token can not be sent to the service: %s", t.toString());
        }
    }

    @Override
    protected void actionSuccess(RoutingContext event) {
        event.response().setStatusCode(200);
        String result = (String) event.get("result");
        if (result != null) {
            event.response().end(result);
        }
    }
}
