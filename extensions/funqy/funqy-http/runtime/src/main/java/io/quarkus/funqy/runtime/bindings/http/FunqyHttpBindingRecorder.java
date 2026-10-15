package io.quarkus.funqy.runtime.bindings.http;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.funqy.runtime.FunctionConstructor;
import io.quarkus.funqy.runtime.FunctionInvoker;
import io.quarkus.funqy.runtime.FunctionRecorder;
import io.quarkus.funqy.runtime.query.QueryObjectMapper;
import io.quarkus.funqy.runtime.query.QueryReader;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Provides the runtime methods to bootstrap Quarkus Funq
 */
@Recorder
public class FunqyHttpBindingRecorder {
    private static ObjectMapper objectMapper;
    private static QueryObjectMapper queryMapper;

    public void init() {
        objectMapper = getJsonMapperBuilder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                .build();
        queryMapper = new QueryObjectMapper();
        for (FunctionInvoker invoker : FunctionRecorder.registry.invokers()) {
            if (invoker.hasInput()) {
                JavaType javaInputType = objectMapper.constructType(invoker.getInputType());
                ObjectReader reader = objectMapper.readerFor(javaInputType);
                QueryReader queryReader = queryMapper.readerFor(invoker.getInputType());
                invoker.getBindingContext().put(ObjectReader.class.getName(), reader);
                invoker.getBindingContext().put(QueryReader.class.getName(), queryReader);
            }
            if (invoker.hasOutput()) {
                JavaType javaOutputType = objectMapper.constructType(invoker.getOutputType());
                ObjectWriter writer = objectMapper.writerFor(javaOutputType);
                invoker.getBindingContext().put(ObjectWriter.class.getName(), writer);
            }
        }
    }

    private JsonMapper.Builder getJsonMapperBuilder() {
        InstanceHandle<JsonMapper> instance = Arc.container().instance(JsonMapper.class);
        if (instance.isAvailable()) {
            return instance.get().rebuild();
        }
        return JsonMapper.builder();
    }

    public Handler<RoutingContext> start(String contextPath,
            Supplier<Vertx> vertx,
            ShutdownContext shutdown,
            BeanContainer beanContainer,
            Executor executor) {

        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                FunctionConstructor.CONTAINER = null;
                objectMapper = null;
            }
        });
        FunctionConstructor.CONTAINER = beanContainer;

        return new VertxRequestHandler(vertx.get(), beanContainer, contextPath, executor);
    }
}
