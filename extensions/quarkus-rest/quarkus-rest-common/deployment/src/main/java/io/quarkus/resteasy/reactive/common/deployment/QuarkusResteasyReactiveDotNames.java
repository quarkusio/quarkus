package io.quarkus.resteasy.reactive.common.deployment;

import org.jboss.jandex.DotName;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class QuarkusResteasyReactiveDotNames {

    public static final DotName HTTP_SERVER_REQUEST = DotName.createSimple(HttpServerRequest.class.getName());
    public static final DotName HTTP_SERVER_RESPONSE = DotName.createSimple(HttpServerResponse.class.getName());
}
