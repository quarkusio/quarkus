package io.quarkus.reactivemessaging.http;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.reactivemessaging.http.config.HttpStreamConfig;
import io.quarkus.reactivemessaging.http.config.ReactiveHttpConfig;
import io.quarkus.reactivemessaging.http.config.WebsocketStreamConfig;
import io.reactivex.processors.BehaviorProcessor;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 01/10/2019
 */
@Singleton
public class ReactiveHttpHandlerBean {

    @Inject
    ReactiveHttpConfig config;

    private final Map<String, BehaviorProcessor> processors = new HashMap<>();
    private final Map<String, BehaviorProcessor> websocketProcessors = new HashMap<>();

    public void handleHttp(RoutingContext event) {
        String path = event.normalisedPath();
        HttpMethod method = event.request().method();
        BehaviorProcessor processor = processors.get(key(path, method));
        if (processor != null) {
            processor.onNext(new HttpMessage(event.getBody()));
            event.response().setStatusCode(202).end();
        } else {
            event.response().setStatusCode(404).end("Handler found but no config for the current path/config pair");
        }
    }

    public void handleWebsocket(RoutingContext event) {
        String path = event.normalisedPath();
        BehaviorProcessor processor = websocketProcessors.get(path);
        HttpMethod method = event.request().method();
        if (processor != null) {
            event.request().upgrade().handler(b -> {
                processor.onNext(new HttpMessage(b));
            });
        } else {
            event.response().setStatusCode(404).end("Handler found but no config for the current path/config pair");
        }
    }

    public <T> BehaviorProcessor<HttpMessage> getProcessor(String path, HttpMethod method) {
        BehaviorProcessor processor = processors.get(key(path, method));
        if (processor == null) {
            throw new IllegalStateException("No incoming stream defined for path " + path + " and method " + method);
        }
        return processor;
    }

    public BehaviorProcessor<HttpMessage> getWebsocketProcessor(String path) {
        BehaviorProcessor processor = websocketProcessors.get(path);
        if (processor == null) {
            throw new IllegalStateException("No incoming stream defined for path " + path);
        }
        return processor;
    }

    @PostConstruct
    public void init() {
        config.getHttpConfigs()
                .forEach(this::addHttpProcessor);
        config.getWebsocketConfigs()
                .forEach(this::addWebsocketProcessor);
    }

    private void addWebsocketProcessor(WebsocketStreamConfig streamConfig) {
        BehaviorProcessor<Object> processor = BehaviorProcessor.create();
        BehaviorProcessor previousProcessor = websocketProcessors.put(streamConfig.path, processor);
        if (previousProcessor != null) {
            throw new IllegalStateException("Duplicate incoming streams defined for path " + streamConfig.path);
        }
    }

    private void addHttpProcessor(HttpStreamConfig streamConfig) {
        BehaviorProcessor<Object> processor = BehaviorProcessor.create();
        BehaviorProcessor previousProcessor = processors.put(key(streamConfig.path, streamConfig.method), processor);
        if (previousProcessor != null) {
            throw new IllegalStateException("Duplicate incoming streams defined for path " + streamConfig.path
                    + " and method " + streamConfig.method);
        }
    }

    private String key(String path, HttpMethod method) {
        return String.format("%s:%s", path, method);
    }
}
