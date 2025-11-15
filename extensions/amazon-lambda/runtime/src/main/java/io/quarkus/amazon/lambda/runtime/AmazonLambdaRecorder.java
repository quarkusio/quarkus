package io.quarkus.amazon.lambda.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.amazon.lambda.runtime.handlers.CollectionInputReader;
import io.quarkus.amazon.lambda.runtime.handlers.S3EventInputReader;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Used for Amazon Lambda java runtime
 *
 */
@Recorder
public class AmazonLambdaRecorder {
    private static Class<? extends RequestHandler<?, ?>> handlerClass;
    static Class<? extends RequestStreamHandler> streamHandlerClass;
    private static BeanContainer beanContainer;
    private static LambdaInputReader objectReader;
    private static LambdaOutputWriter objectWriter;
    protected static Set<Class<?>> expectedExceptionClasses;

    private final RuntimeValue<LambdaConfig> runtimeConfig;

    public AmazonLambdaRecorder(RuntimeValue<LambdaConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public void setStreamHandlerClass(Class<? extends RequestStreamHandler> handler) {
        streamHandlerClass = handler;
    }

    static void initializeHandlerClass(RequestHandlerDefinition requestHandlerDefinition) {
        handlerClass = requestHandlerDefinition.handlerClass();
        ObjectMapper objectMapper = AmazonLambdaMapperRecorder.objectMapper;

        if (requestHandlerDefinition.inputType().equals(S3Event.class)) {
            objectReader = new S3EventInputReader(objectMapper);
        } else if (Collection.class.isAssignableFrom(requestHandlerDefinition.inputType())) {
            // we have to use reflection to figure out the element generic type
            try {
                Method handleRequestMethod = requestHandlerDefinition.handleRequestMethodHostClass().getMethod("handleRequest",
                        requestHandlerDefinition.inputType(), Context.class);
                objectReader = new CollectionInputReader<>(objectMapper, handleRequestMethod.getGenericParameterTypes()[0]);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to find handleRequest method in " + handlerClass.getName(), e);
            }
        } else {
            objectReader = new JacksonInputReader(objectMapper.readerFor(requestHandlerDefinition.inputType()));
        }

        objectWriter = new JacksonOutputWriter(objectMapper.writerFor(requestHandlerDefinition.outputType()));
    }

    public void setBeanContainer(BeanContainer container) {
        beanContainer = container;
    }

    /**
     * Called by JVM handler wrapper
     *
     * @param inputStream
     * @param outputStream
     * @param context
     * @throws IOException
     */
    public static void handle(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        if (streamHandlerClass != null) {
            RequestStreamHandler handler = beanContainer.beanInstance(streamHandlerClass);
            handler.handleRequest(inputStream, outputStream, context);
        } else {
            Object request = objectReader.readValue(inputStream);
            RequestHandler handler = beanContainer.beanInstance(handlerClass);
            Object response = handler.handleRequest(request, context);
            objectWriter.writeValue(outputStream, response);
        }
    }

    public void chooseHandlerClass(List<RequestHandlerDefinition> unnamedHandlerDefinitions,
            Map<String, RequestHandlerDefinition> namedHandlerDefinitions,
            List<Class<? extends RequestStreamHandler>> unnamedStreamHandlerClasses,
            Map<String, Class<? extends RequestStreamHandler>> namedStreamHandlerClasses) {

        RequestHandlerDefinition handlerDefinition = null;
        Class<? extends RequestStreamHandler> handlerStreamClass = null;
        if (runtimeConfig.getValue().handler().isPresent()) {
            handlerDefinition = namedHandlerDefinitions.get(runtimeConfig.getValue().handler().get());
            handlerStreamClass = namedStreamHandlerClasses.get(runtimeConfig.getValue().handler().get());

            if (handlerDefinition == null && handlerStreamClass == null) {
                String errorMessage = "Unable to find handler class with name " + runtimeConfig.getValue().handler().get()
                        + " make sure there is a handler class in the deployment with the correct @Named annotation";
                throw new IllegalStateException(errorMessage);
            }
        } else {
            int unnamedTotal = unnamedHandlerDefinitions.size() + unnamedStreamHandlerClasses.size();
            int namedTotal = namedHandlerDefinitions.size() + namedStreamHandlerClasses.size();

            if (unnamedTotal > 1 || namedTotal > 1 || (unnamedTotal > 0 && namedTotal > 0)) {
                String errorMessage = "Multiple handler classes, either specify the quarkus.lambda.handler property, or make sure there is only a single "
                        + RequestHandler.class.getName() + " or, " + RequestStreamHandler.class.getName()
                        + " implementation in the deployment";
                throw new IllegalStateException(errorMessage);
            } else if (unnamedTotal == 0 && namedTotal == 0) {
                String errorMessage = "Unable to find handler class, make sure your deployment includes a single "
                        + RequestHandler.class.getName() + " or, " + RequestStreamHandler.class.getName() + " implementation";
                throw new IllegalStateException(errorMessage);
            } else if ((unnamedTotal + namedTotal) == 1) {
                if (!unnamedHandlerDefinitions.isEmpty()) {
                    handlerDefinition = unnamedHandlerDefinitions.get(0);
                } else if (!namedHandlerDefinitions.isEmpty()) {
                    handlerDefinition = namedHandlerDefinitions.values().iterator().next();
                } else if (!unnamedStreamHandlerClasses.isEmpty()) {
                    handlerStreamClass = unnamedStreamHandlerClasses.get(0);
                } else if (!namedStreamHandlerClasses.isEmpty()) {
                    handlerStreamClass = namedStreamHandlerClasses.values().iterator().next();
                }
            }
        }

        if (handlerStreamClass != null) {
            setStreamHandlerClass(handlerStreamClass);
        } else {
            initializeHandlerClass(handlerDefinition);
        }
    }

    @SuppressWarnings("rawtypes")
    public void startPollLoop(ShutdownContext context, LaunchMode launchMode) {
        AbstractLambdaPollLoop loop = new AbstractLambdaPollLoop(AmazonLambdaMapperRecorder.objectMapper,
                AmazonLambdaMapperRecorder.cognitoIdReader, AmazonLambdaMapperRecorder.clientCtxReader, launchMode) {

            @Override
            protected Object processRequest(Object input, AmazonLambdaContext context) throws Exception {
                RequestHandler handler = beanContainer.beanInstance(handlerClass);
                return handler.handleRequest(input, context);
            }

            @Override
            protected LambdaInputReader getInputReader() {
                return objectReader;
            }

            @Override
            protected LambdaOutputWriter getOutputWriter() {
                return objectWriter;
            }

            @Override
            protected boolean isStream() {
                return streamHandlerClass != null;
            }

            @Override
            protected void processRequest(InputStream input, OutputStream output, AmazonLambdaContext context)
                    throws Exception {
                RequestStreamHandler handler = beanContainer.beanInstance(streamHandlerClass);
                handler.handleRequest(input, output, context);

            }

            @Override
            protected boolean shouldLog(Exception e) {
                return expectedExceptionClasses.stream().noneMatch(clazz -> clazz.isAssignableFrom(e.getClass()));
            }
        };
        loop.startPollLoop(context);

    }

    public record RequestHandlerDefinition(Class<? extends RequestHandler<?, ?>> handlerClass,
            Class<?> handleRequestMethodHostClass, Class<?> inputType, Class<?> outputType) {
    }
}
