package io.quarkus.resteasy.reactive.common.deployment;

import org.jboss.jandex.DotName;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class QuarkusResteasyReactiveDotNames {

    public static final DotName HTTP_SERVER_REQUEST = DotName.createSimple(HttpServerRequest.class.getName());
    public static final DotName HTTP_SERVER_RESPONSE = DotName.createSimple(HttpServerResponse.class.getName());

    // TODO: fix this hack by moving all the logic that handles this annotation to the server processor
    public static final DotName SERVER_EXCEPTION_MAPPER = DotName
            .createSimple("org.jboss.resteasy.reactive.server.ServerExceptionMapper");
}
