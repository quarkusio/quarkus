package io.quarkus.funqy.runtime.bindings.knative.events;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import io.quarkus.funqy.runtime.FunctionConstructor;
import io.quarkus.funqy.runtime.FunctionInvoker;
import io.quarkus.funqy.runtime.FunctionRecorder;
import io.quarkus.funqy.runtime.FunqyConfig;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

/**
 * Provides the runtime methods to bootstrap Quarkus Funq
 */
@Recorder
public class KnativeEventsBindingRecorder {
    private static final Logger log = Logger.getLogger(KnativeEventsBindingRecorder.class);

    private static ObjectMapper objectMapper;
    private static Map<String, FunctionInvoker> typeTriggers;

    public static final String RESPONSE_TYPE = "response.cloud.event.type";
    public static final String RESPONSE_SOURCE = "response.cloud.event.source";

    public void init() {
        typeTriggers = new HashMap<>();
        objectMapper = getObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        for (FunctionInvoker invoker : FunctionRecorder.registry.invokers()) {
            Method method = invoker.getMethod();
            CloudEventMapping annotation = method.getAnnotation(CloudEventMapping.class);
            if (annotation != null && !annotation.trigger().isEmpty()) {
                typeTriggers.put(annotation.trigger(), invoker);
            } else {
                typeTriggers.put(invoker.getName(), invoker);
            }

            if (invoker.hasInput()) {
                ObjectReader reader = objectMapper.readerFor(invoker.getInputType());
                invoker.getBindingContext().put(ObjectReader.class.getName(), reader);
            }
            if (invoker.hasOutput()) {
                ObjectWriter writer = objectMapper.writerFor(invoker.getOutputType());
                invoker.getBindingContext().put(ObjectWriter.class.getName(), writer);

                String functionName = invoker.getName();
                if (annotation != null && !annotation.responseType().isEmpty()) {
                    invoker.getBindingContext().put(RESPONSE_TYPE, annotation.responseType());
                } else {
                    invoker.getBindingContext().put(RESPONSE_TYPE, functionName + ".output");
                }
                if (annotation != null && !annotation.responseSource().isEmpty()) {
                    invoker.getBindingContext().put(RESPONSE_SOURCE, annotation.responseSource());
                } else {
                    invoker.getBindingContext().put(RESPONSE_SOURCE, functionName);
                }
            }
        }
    }

    private ObjectMapper getObjectMapper() {
        InstanceHandle<ObjectMapper> instance = Arc.container().instance(ObjectMapper.class);
        if (instance.isAvailable()) {
            return instance.get().copy();
        }
        return new ObjectMapper();
    }

    public Consumer<Route> start(
            FunqyConfig funqyConfig,
            FunqyKnativeEventsConfig eventsConfig,
            Supplier<Vertx> vertx,
            ShutdownContext shutdown,
            BeanContainer beanContainer,
            Executor executor) {

        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                FunctionConstructor.CONTAINER = null;
                objectMapper = null;
                typeTriggers = null;
            }
        });

        FunctionConstructor.CONTAINER = beanContainer;

        // This needs to happen in start at RUNTIME so that
        // mappings can be overriden by environment variables
        FunctionInvoker defaultInvoker = null;
        if (funqyConfig.export.isPresent()) {
            defaultInvoker = FunctionRecorder.registry.matchInvoker(funqyConfig.export.get());
            if (defaultInvoker == null) {
                throw new RuntimeException("quarkus.funqy.export value does not map a function: " + funqyConfig.export.get());
            }

        }

        if (eventsConfig.mapping != null) {
            for (Map.Entry<String, FunqyKnativeEventsConfig.FunctionMapping> entry : eventsConfig.mapping.entrySet()) {
                String functionName = entry.getKey();
                FunctionInvoker invoker = FunctionRecorder.registry.matchInvoker(functionName);
                if (invoker == null) {
                    throw new RuntimeException("knative-events.function-mapping does not map to a function: " + functionName);
                }
                FunqyKnativeEventsConfig.FunctionMapping mapping = entry.getValue();
                if (mapping.trigger.isPresent()) {
                    typeTriggers.put(mapping.trigger.get(), invoker);
                }
                if (invoker.hasOutput()) {
                    if (mapping.responseSource.isPresent()) {
                        invoker.getBindingContext().put(RESPONSE_SOURCE, mapping.responseSource.get());
                    }
                    if (mapping.responseType.isPresent()) {
                        invoker.getBindingContext().put(RESPONSE_TYPE, mapping.responseType.get());
                    }
                }
            }

        }

        Handler<RoutingContext> handler = new VertxRequestHandler(vertx.get(), beanContainer, objectMapper, eventsConfig,
                defaultInvoker, typeTriggers, executor);

        return new Consumer<Route>() {

            @Override
            public void accept(Route route) {
                route.handler(handler);
            }
        };
    }
}
