package io.quarkus.funqy.gcp.functions;

import java.io.IOException;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.cloud.functions.Context;

import io.cloudevents.CloudEvent;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.funqy.runtime.FunctionConstructor;
import io.quarkus.funqy.runtime.FunctionInvoker;
import io.quarkus.funqy.runtime.FunctionRecorder;
import io.quarkus.funqy.runtime.FunqyConfig;
import io.quarkus.funqy.runtime.FunqyServerResponse;
import io.quarkus.funqy.runtime.RequestContextImpl;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Provides the runtime methods to bootstrap Quarkus Funqy
 */
@Recorder
public class FunqyCloudFunctionsBindingRecorder {
    private static FunctionInvoker invoker;
    private static BeanContainer beanContainer;
    private static ObjectMapper objectMapper;
    private static ObjectReader reader;
    private static ObjectWriter writer;

    private final RuntimeValue<FunqyConfig> config;

    public FunqyCloudFunctionsBindingRecorder(final RuntimeValue<FunqyConfig> config) {
        this.config = config;
    }

    public void init(BeanContainer bc) {
        beanContainer = bc;
        objectMapper = beanContainer.beanInstance(ObjectMapper.class);

        for (FunctionInvoker invoker : FunctionRecorder.registry.invokers()) {
            if (invoker.hasInput()) {
                JavaType javaInputType = objectMapper.constructType(invoker.getInputType());
                ObjectReader reader = objectMapper.readerFor(javaInputType);
                invoker.getBindingContext().put(ObjectReader.class.getName(), reader);
            }
            if (invoker.hasOutput()) {
                JavaType javaOutputType = objectMapper.constructType(invoker.getOutputType());
                ObjectWriter writer = objectMapper.writerFor(javaOutputType);
                invoker.getBindingContext().put(ObjectWriter.class.getName(), writer);
            }
        }

        FunctionConstructor.CONTAINER = bc;
    }

    public void chooseInvoker() {
        // this is done at Runtime so that we can change it with an environment variable.
        if (config.getValue().export().isPresent()) {
            invoker = FunctionRecorder.registry.matchInvoker(config.getValue().export().get());
            if (invoker == null) {
                throw new RuntimeException(
                        "quarkus.funqy.export does not match a function: " + config.getValue().export().get());
            }
        } else if (FunctionRecorder.registry.invokers().size() == 0) {
            throw new RuntimeException("There are no functions to process lambda");

        } else if (FunctionRecorder.registry.invokers().size() > 1) {
            throw new RuntimeException("Too many functions.  You need to set quarkus.funqy.export");
        } else {
            invoker = FunctionRecorder.registry.invokers().iterator().next();
        }
        if (invoker.hasInput()) {
            reader = (ObjectReader) invoker.getBindingContext().get(ObjectReader.class.getName());
        }
        if (invoker.hasOutput()) {
            writer = (ObjectWriter) invoker.getBindingContext().get(ObjectWriter.class.getName());
        }
    }

    /**
     * Handle RawBackgroundFunction
     *
     * @param event
     * @param context
     */
    public static void handle(String event, Context context) {
        //TODO allow to access the context from the function somehow.
        try {
            Object input = null;
            if (invoker.hasInput()) {
                input = reader.readValue(event);
            }
            FunqyServerResponse response = dispatch(input);

            Object value = response.getOutput().await().indefinitely();
            if (value != null) {
                throw new RuntimeException("A background function cannot return a value");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Handle CloudEventsFunction
     *
     * @param cloudEvent
     */
    public static void handle(CloudEvent cloudEvent) {
        FunqyServerResponse response = dispatch(cloudEvent);

        Object value = response.getOutput().await().indefinitely();
        if (value != null) {
            throw new RuntimeException("A background function cannot return a value");
        }
    }

    private static FunqyServerResponse dispatch(Object input) {
        ManagedContext requestContext = beanContainer.requestContext();
        requestContext.activate();
        try {
            FunqyRequestImpl funqyRequest = new FunqyRequestImpl(new RequestContextImpl(), input);
            FunqyResponseImpl funqyResponse = new FunqyResponseImpl();
            invoker.invoke(funqyRequest, funqyResponse);
            return funqyResponse;
        } finally {
            if (requestContext.isActive()) {
                requestContext.terminate();
            }
        }
    }

}
