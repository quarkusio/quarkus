package io.quarkus.funqy.runtime.bindings.knative.events;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.impl.Reflections;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import io.quarkus.funqy.knative.events.EventAttribute;
import io.quarkus.funqy.runtime.FunctionConstructor;
import io.quarkus.funqy.runtime.FunctionInvoker;
import io.quarkus.funqy.runtime.FunctionRecorder;
import io.quarkus.funqy.runtime.FunqyConfig;
import io.quarkus.funqy.runtime.bindings.knative.events.filters.CEAttributeLiteralEqualsFilter;
import io.quarkus.funqy.runtime.query.QueryObjectMapper;
import io.quarkus.funqy.runtime.query.QueryReader;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * Provides the runtime methods to bootstrap Quarkus Funq
 */
@Recorder
public class KnativeEventsBindingRecorder {
    private static final Logger log = Logger.getLogger(KnativeEventsBindingRecorder.class);

    private static ObjectMapper objectMapper;
    private static QueryObjectMapper queryMapper;
    private static Map<String, Collection<FunctionInvoker>> typeTriggers;
    private static Map<String, List<Predicate<CloudEvent>>> invokersFilters;

    public static final String RESPONSE_TYPE = "response.cloud.event.type";
    public static final String RESPONSE_SOURCE = "response.cloud.event.source";
    public static final String INPUT_CE_DATA_TYPE = "io.quarkus.funqy.knative.events.INPUT_CE_DATA_TYPE";
    public static final String OUTPUT_CE_DATA_TYPE = "io.quarkus.funqy.knative.events.OUTPUT_CE_DATA_TYPE";
    public static final String DATA_OBJECT_READER = ObjectReader.class.getName() + "_DATA_OBJECT_READER";
    public static final String DATA_OBJECT_WRITER = ObjectWriter.class.getName() + "_DATA_OBJECT_WRITER";

    public void init() {
        typeTriggers = new HashMap<>();
        invokersFilters = new HashMap<>();
        objectMapper = getObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        queryMapper = new QueryObjectMapper();
        for (FunctionInvoker invoker : FunctionRecorder.registry.invokers()) {
            Method method = invoker.getMethod();
            String trigger;
            CloudEventMapping annotation = method.getAnnotation(CloudEventMapping.class);
            final List<Predicate<CloudEvent>> filter;
            if (annotation != null && !annotation.trigger().isEmpty()) {
                trigger = annotation.trigger();
                filter = filter(invoker.getName(), annotation);
            } else {
                trigger = invoker.getName();
                filter = Collections.emptyList();
            }
            invokersFilters.put(invoker.getName(), filter);
            typeTriggers.compute(trigger, (k, v) -> {
                if (v == null) {
                    v = new ArrayList<>();
                }
                // validate if there are no conflicts for the same type (trigger) and defined filters
                // as resolution based on trigger (ce-type) and optional filters (on ce-attributes) can return only
                // one function invoker
                if (v.stream().anyMatch(i -> hasSameFilters(i.getName(), invokersFilters.get(i.getName()), filter))) {
                    throw new IllegalStateException("Function for trigger '" + trigger + "' has multiple matching invokers");
                }

                v.add(invoker);
                return v;
            });

            if (invoker.hasInput()) {
                Type inputType = invoker.getInputType();

                if (CloudEvent.class.equals(Reflections.getRawType(inputType))) {
                    if (inputType instanceof ParameterizedType) {
                        Type[] params = ((ParameterizedType) inputType).getActualTypeArguments();
                        if (params.length == 1) {
                            inputType = params[0];
                            invoker.getBindingContext().put(INPUT_CE_DATA_TYPE, inputType);
                        }
                    } else {
                        throw new RuntimeException("When using CloudEvent<> generic parameter must be used.");
                    }
                }

                JavaType javaInputType = objectMapper.constructType(inputType);
                ObjectReader reader = objectMapper.readerFor(javaInputType);
                invoker.getBindingContext().put(DATA_OBJECT_READER, reader);
                QueryReader queryReader = queryMapper.readerFor(inputType);
                invoker.getBindingContext().put(QueryReader.class.getName(), queryReader);
            }
            if (invoker.hasOutput()) {
                Type outputType = invoker.getOutputType();

                if (CloudEvent.class.equals(Reflections.getRawType(outputType))) {
                    if (outputType instanceof ParameterizedType) {
                        Type[] params = ((ParameterizedType) outputType).getActualTypeArguments();
                        if (params.length == 1) {
                            outputType = params[0];
                            invoker.getBindingContext().put(OUTPUT_CE_DATA_TYPE, outputType);
                        }
                    } else {
                        throw new RuntimeException("When using CloudEvent<> generic parameter must be used.");
                    }
                }

                JavaType outputJavaType = objectMapper.constructType(outputType);
                ObjectWriter writer = objectMapper.writerFor(outputJavaType);
                invoker.getBindingContext().put(DATA_OBJECT_WRITER, writer);

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

    public Handler<RoutingContext> start(
            String rootPath,
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
                    typeTriggers.compute(mapping.trigger.get(), (k, v) -> {
                        if (v == null) {
                            v = new ArrayList<>();
                        }
                        v.add(invoker);
                        return v;
                    });
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

        Handler<RoutingContext> handler = new VertxRequestHandler(vertx.get(), rootPath, beanContainer, objectMapper,
                eventsConfig,
                defaultInvoker, typeTriggers, invokersFilters, executor);

        return handler;
    }

    private List<Predicate<CloudEvent>> filter(String functionName, CloudEventMapping mapping) {

        if (mapping.attributes() == null || mapping.attributes().length == 0) {
            return Collections.emptyList();
        }
        List<Predicate<CloudEvent>> filters = new ArrayList<>();
        for (EventAttribute attribute : mapping.attributes()) {
            Objects.requireNonNull(attribute.name(),
                    "Attribute name of the EventAttribure on function " + functionName + " is required");
            Objects.requireNonNull(attribute.value(),
                    "Attribute name of the EventAttribure on function " + functionName + " is required");

            filters.add(new CEAttributeLiteralEqualsFilter(attribute.name(), attribute.value()));

        }

        return filters;
    }

    private boolean hasSameFilters(String name, List<Predicate<CloudEvent>> one, List<Predicate<CloudEvent>> two) {

        final List<Predicate<CloudEvent>> first = one != null ? one : Collections.emptyList();
        final List<Predicate<CloudEvent>> second = two != null ? two : Collections.emptyList();

        // empty set is sub-set of any set
        if (first.size() <= 0 || second.size() <= 0) {
            log.warn("Invoker " + name + " has multiple matching filters " + one + " " + two);
            return true;
        }

        boolean result = first.size() <= second.size() ? second.containsAll(first) : first.containsAll(second);
        if (result) {
            log.warn("Invoker " + name + " has multiple matching filters " + one + " " + two);
        }
        return result;
    }
}
