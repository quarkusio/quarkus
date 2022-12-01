package io.quarkus.cache.runtime.devconsole;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.Optional;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.cache.runtime.CaffeineCacheSupplier;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheImpl;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.devmode.Json;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class CacheDevConsoleRecorder {
    public Handler<RoutingContext> clearCacheHandler() {
        return new DevConsolePostHandler() {

            @Override
            protected void handlePost(RoutingContext event, MultiMap form) {
                String cacheName = form.get("name");
                Optional<Cache> cache = CaffeineCacheSupplier.cacheManager().getCache(cacheName);
                if (cache.isPresent() && cache.get() instanceof CaffeineCache) {
                    CaffeineCacheImpl caffeineCache = (CaffeineCacheImpl) cache.get();

                    String action = form.get("action");
                    if (action.equalsIgnoreCase("clearCache")) {
                        caffeineCache.invalidateAll().subscribe().with(ignored -> {
                            endResponse(event, OK, createResponseMessage(caffeineCache));
                        });
                    } else if (action.equalsIgnoreCase("refresh")) {
                        endResponse(event, OK, createResponseMessage(caffeineCache));
                    } else {
                        String errorMessage = "Invalid action: " + action;
                        endResponse(event, INTERNAL_SERVER_ERROR, createResponseError(cacheName, errorMessage));
                    }
                } else {
                    String errorMessage = "Cache for " + cacheName + " not found";
                    endResponse(event, NOT_FOUND, createResponseError(cacheName, errorMessage));
                }
            }

            private void endResponse(RoutingContext event, HttpResponseStatus status, String message) {
                event.response().setStatusCode(status.code());
                event.response().end(message);
            }

            @Override
            protected void actionSuccess(RoutingContext event) {
            }

            private String createResponseMessage(CaffeineCacheImpl cache) {
                Json.JsonObjectBuilder object = Json.object();
                object.put("name", cache.getName());
                object.put("size", cache.getSize());
                return object.build();
            }

            private String createResponseError(String name, String error) {
                Json.JsonObjectBuilder object = Json.object();
                object.put("name", name);
                object.put("error", error);
                return object.build();
            }
        };
    }
}
