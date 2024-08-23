package io.quarkus.vertx.http.runtime.options;

import static io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.setCurrentContextSafe;
import static io.quarkus.vertx.http.runtime.TrustedProxyCheck.allowAll;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.vertx.http.runtime.FilterConfig;
import io.quarkus.vertx.http.runtime.ForwardedProxyHandler;
import io.quarkus.vertx.http.runtime.ForwardedServerRequestWrapper;
import io.quarkus.vertx.http.runtime.ForwardingProxyOptions;
import io.quarkus.vertx.http.runtime.HeaderConfig;
import io.quarkus.vertx.http.runtime.ProxyConfig;
import io.quarkus.vertx.http.runtime.ResumingRequestWrapper;
import io.quarkus.vertx.http.runtime.RouteConstants;
import io.quarkus.vertx.http.runtime.ServerLimitsConfig;
import io.quarkus.vertx.http.runtime.TrustedProxyCheck;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HttpServerCommonHandlers {
    public static void enforceMaxBodySize(ServerLimitsConfig limits, Router httpRouteRouter) {
        if (limits.maxBodySize.isPresent()) {
            long limit = limits.maxBodySize.get().asLongValue();
            Long limitObj = limit;
            httpRouteRouter.route().order(RouteConstants.ROUTE_ORDER_UPLOAD_LIMIT).handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext event) {
                    String lengthString = event.request().headers().get(HttpHeaderNames.CONTENT_LENGTH);

                    if (lengthString != null) {
                        long length = Long.parseLong(lengthString);
                        if (length > limit) {
                            event.response().headers().add(HttpHeaderNames.CONNECTION, "close");
                            event.response().setStatusCode(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE.code());
                            event.response().endHandler(new Handler<Void>() {
                                @Override
                                public void handle(Void e) {
                                    event.request().connection().close();
                                }
                            });
                            event.response().end();
                            return;
                        }
                    } else {
                        event.put(VertxHttpRecorder.MAX_REQUEST_SIZE_KEY, limitObj);
                    }
                    event.next();
                }
            });
        }
    }

    public static Handler<HttpServerRequest> enforceDuplicatedContext(Handler<HttpServerRequest> delegate) {
        return new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest event) {
                if (!VertxContext.isOnDuplicatedContext()) {
                    // Vert.x should call us on a duplicated context.
                    // But in the case of pipelined requests, it does not.
                    // See https://github.com/quarkusio/quarkus/issues/24626.
                    Context context = VertxContext.createNewDuplicatedContext();
                    context.runOnContext(new Handler<Void>() {
                        @Override
                        public void handle(Void x) {
                            setCurrentContextSafe(true);
                            delegate.handle(new ResumingRequestWrapper(event));
                        }
                    });
                } else {
                    setCurrentContextSafe(true);
                    delegate.handle(new ResumingRequestWrapper(event));
                }
            }
        };
    }

    public static Handler<HttpServerRequest> applyProxy(ProxyConfig proxyConfig, Handler<HttpServerRequest> root,
            Supplier<Vertx> vertx) {
        if (proxyConfig.proxyAddressForwarding) {
            final ForwardingProxyOptions forwardingProxyOptions = ForwardingProxyOptions.from(proxyConfig);
            final TrustedProxyCheck.TrustedProxyCheckBuilder proxyCheckBuilder = forwardingProxyOptions.trustedProxyCheckBuilder;
            if (proxyCheckBuilder == null) {
                // no proxy check => we do not restrict who can send `X-Forwarded` or `X-Forwarded-*` headers
                final TrustedProxyCheck allowAllProxyCheck = allowAll();
                return new Handler<HttpServerRequest>() {
                    @Override
                    public void handle(HttpServerRequest event) {
                        root.handle(new ForwardedServerRequestWrapper(event, forwardingProxyOptions, allowAllProxyCheck));
                    }
                };
            } else {
                // restrict who can send `Forwarded`, `X-Forwarded` or `X-Forwarded-*` headers
                return new ForwardedProxyHandler(proxyCheckBuilder, vertx, root, forwardingProxyOptions);
            }
        }
        return root;
    }

    public static void applyFilters(Map<String, FilterConfig> filtersInConfig, Router httpRouteRouter) {
        if (!filtersInConfig.isEmpty()) {
            for (var entry : filtersInConfig.entrySet()) {
                var filterConfig = entry.getValue();
                var matches = filterConfig.matches;
                var order = filterConfig.order.orElse(Integer.MIN_VALUE);
                var methods = filterConfig.methods;
                var headers = filterConfig.header;
                if (methods.isEmpty()) {
                    httpRouteRouter.routeWithRegex(matches)
                            .order(order)
                            .handler(new Handler<RoutingContext>() {
                                @Override
                                public void handle(RoutingContext event) {
                                    addFilterHeaders(event, headers);
                                    event.next();
                                }

                            });
                } else {
                    for (var method : methods.get()) {
                        httpRouteRouter.routeWithRegex(HttpMethod.valueOf(method.toUpperCase(Locale.ROOT)), matches)
                                .order(order)
                                .handler(new Handler<RoutingContext>() {
                                    @Override
                                    public void handle(RoutingContext event) {
                                        addFilterHeaders(event, headers);
                                        event.next();
                                    }
                                });
                    }
                }
            }
        }
    }

    private static void addFilterHeaders(RoutingContext event, Map<String, String> headers) {
        for (var entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            MultiMap responseHeaders = event.response().headers();
            List<String> oldValues = responseHeaders.getAll(key);
            if (oldValues.isEmpty()) {
                responseHeaders.set(key, value);
            } else {
                // we need to make sure the new value is not duplicated
                var newValues = new LinkedHashSet<String>(oldValues);
                boolean added = newValues.add(value);
                if (added) {
                    responseHeaders.set(key, newValues);
                } else {
                    // we don't need to do anything here as the value was already in the set
                }
            }
        }
    }

    public static void applyHeaders(Map<String, HeaderConfig> headers, Router httpRouteRouter) {
        if (!headers.isEmpty()) {
            // Creates a handler for each header entry
            for (Map.Entry<String, HeaderConfig> entry : headers.entrySet()) {
                var name = entry.getKey();
                var config = entry.getValue();
                if (config.methods.isEmpty()) {
                    httpRouteRouter.route(config.path)
                            .order(RouteConstants.ROUTE_ORDER_HEADERS)
                            .handler(new Handler<RoutingContext>() {
                                @Override
                                public void handle(RoutingContext event) {
                                    event.response().headers().set(name, config.value);
                                    event.next();
                                }
                            });
                } else {
                    for (String method : config.methods.get()) {
                        httpRouteRouter.route(HttpMethod.valueOf(method.toUpperCase(Locale.ROOT)), config.path)
                                .order(RouteConstants.ROUTE_ORDER_HEADERS)
                                .handler(new Handler<RoutingContext>() {
                                    @Override
                                    public void handle(RoutingContext event) {
                                        event.response().headers().add(name, config.value);
                                        event.next();
                                    }
                                });
                    }
                }
            }
        }
    }
}
