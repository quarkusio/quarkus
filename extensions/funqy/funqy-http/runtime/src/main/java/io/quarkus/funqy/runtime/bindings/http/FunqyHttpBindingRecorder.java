package io.quarkus.funqy.runtime.bindings.http;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

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

/**
 * Provides the runtime methods to bootstrap Quarkus Funq
 */
@Recorder
public class FunqyHttpBindingRecorder {
    private static ObjectMapper objectMapper;
    private static QueryObjectMapper queryMapper;

    public void init() {
        objectMapper = getObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        queryMapper = new QueryObjectMapper();
        for (FunctionInvoker invoker : FunctionRecorder.registry.invokers()) {
            if (invoker.hasInput()) {
                ObjectReader reader = objectMapper.readerFor(invoker.getInputType());
                QueryReader queryReader = queryMapper.readerFor(invoker.getInputType(), invoker.getInputGenericType());
                invoker.getBindingContext().put(ObjectReader.class.getName(), reader);
                invoker.getBindingContext().put(QueryReader.class.getName(), queryReader);
            }
            if (invoker.hasOutput()) {
                ObjectWriter writer = objectMapper.writerFor(invoker.getOutputType());
                invoker.getBindingContext().put(ObjectWriter.class.getName(), writer);
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
