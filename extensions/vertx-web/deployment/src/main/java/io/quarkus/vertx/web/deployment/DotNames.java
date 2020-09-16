package io.quarkus.vertx.web.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.vertx.web.Body;
import io.quarkus.vertx.web.Header;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.quarkus.vertx.web.RouteFilter;
import io.quarkus.vertx.web.RoutingExchange;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

final class DotNames {

    static final DotName UNI = DotName.createSimple(Uni.class.getName());
    static final DotName MULTI = DotName.createSimple(Multi.class.getName());
    static final DotName BUFFER = DotName.createSimple(Buffer.class.getName());
    static final DotName RX_HTTP_SERVER_RESPONSE = DotName
            .createSimple(io.vertx.reactivex.core.http.HttpServerResponse.class.getName());
    static final DotName RX_HTTP_SERVER_REQUEST = DotName
            .createSimple(io.vertx.reactivex.core.http.HttpServerRequest.class.getName());
    static final DotName HTTP_SERVER_RESPONSE = DotName.createSimple(HttpServerResponse.class.getName());
    static final DotName HTTP_SERVER_REQUEST = DotName.createSimple(HttpServerRequest.class.getName());
    static final DotName ROUTING_EXCHANGE = DotName.createSimple(RoutingExchange.class.getName());
    static final DotName RX_ROUTING_CONTEXT = DotName
            .createSimple(io.vertx.reactivex.ext.web.RoutingContext.class.getName());
    static final DotName ROUTING_CONTEXT = DotName.createSimple(RoutingContext.class.getName());
    static final DotName BODY = DotName.createSimple(Body.class.getName());
    static final DotName HEADER = DotName.createSimple(Header.class.getName());
    static final DotName PARAM = DotName.createSimple(Param.class.getName());
    static final DotName ROUTE_BASE = DotName.createSimple(RouteBase.class.getName());
    static final DotName ROUTE_FILTER = DotName.createSimple(RouteFilter.class.getName());
    static final DotName ROUTES = DotName.createSimple(Route.Routes.class.getName());
    static final DotName ROUTE = DotName.createSimple(Route.class.getName());
    static final DotName JSON_OBJECT = DotName.createSimple(JsonObject.class.getName());
    static final DotName JSON_ARRAY = DotName.createSimple(JsonArray.class.getName());
    static final DotName LIST = DotName.createSimple(List.class.getName());

}
