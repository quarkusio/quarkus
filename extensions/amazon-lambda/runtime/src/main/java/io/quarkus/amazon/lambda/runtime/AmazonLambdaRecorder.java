package io.quarkus.amazon.lambda.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.amazon.lambda.runtime.RequestHandlerDefinitionUtil.RequestHandlerDefinition;
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

    static void initializeHandlerClass(Class<? extends RequestHandler<?, ?>> handler) {
        handlerClass = handler;
        ObjectMapper objectMapper = AmazonLambdaMapperRecorder.objectMapper;
        RequestHandlerDefinition handlerMethodDefinition = RequestHandlerDefinitionUtil.discoverHandlerMethod(handlerClass);

        Class<?> inputType = handlerMethodDefinition.inputOutputTypes().inputType();
        Class<?> outputType = handlerMethodDefinition.inputOutputTypes().outputType();
        if (inputType.equals(S3Event.class)) {
            objectReader = new S3EventInputReader(objectMapper);
        } else if (Collection.class.isAssignableFrom(inputType)) {
            objectReader = new CollectionInputReader<>(objectMapper, handlerMethodDefinition.method());
        } else {
            objectReader = new JacksonInputReader(objectMapper.readerFor(inputType));
        }

        objectWriter = new JacksonOutputWriter(objectMapper.writerFor(outputType));
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

    public void chooseHandlerClass(List<Class<? extends RequestHandler<?, ?>>> unnamedHandlerClasses,
            Map<String, Class<? extends RequestHandler<?, ?>>> namedHandlerClasses,
            List<Class<? extends RequestStreamHandler>> unnamedStreamHandlerClasses,
            Map<String, Class<? extends RequestStreamHandler>> namedStreamHandlerClasses) {

        Class<? extends RequestHandler<?, ?>> handlerClass = null;
        Class<? extends RequestStreamHandler> handlerStreamClass = null;
        if (runtimeConfig.getValue().handler().isPresent()) {
            handlerClass = namedHandlerClasses.get(runtimeConfig.getValue().handler().get());
            handlerStreamClass = namedStreamHandlerClasses.get(runtimeConfig.getValue().handler().get());

            if (handlerClass == null && handlerStreamClass == null) {
                String errorMessage = "Unable to find handler class with name " + runtimeConfig.getValue().handler().get()
                        + " make sure there is a handler class in the deployment with the correct @Named annotation";
                throw new RuntimeException(errorMessage);
            }
        } else {
            int unnamedTotal = unnamedHandlerClasses.size() + unnamedStreamHandlerClasses.size();
            int namedTotal = namedHandlerClasses.size() + namedStreamHandlerClasses.size();

            if (unnamedTotal > 1 || namedTotal > 1 || (unnamedTotal > 0 && namedTotal > 0)) {
                String errorMessage = "Multiple handler classes, either specify the quarkus.lambda.handler property, or make sure there is only a single "
                        + RequestHandler.class.getName() + " or, " + RequestStreamHandler.class.getName()
                        + " implementation in the deployment";
                throw new RuntimeException(errorMessage);
            } else if (unnamedTotal == 0 && namedTotal == 0) {
                String errorMessage = "Unable to find handler class, make sure your deployment includes a single "
                        + RequestHandler.class.getName() + " or, " + RequestStreamHandler.class.getName() + " implementation";
                throw new RuntimeException(errorMessage);
            } else if ((unnamedTotal + namedTotal) == 1) {
                if (!unnamedHandlerClasses.isEmpty()) {
                    handlerClass = unnamedHandlerClasses.get(0);
                } else if (!namedHandlerClasses.isEmpty()) {
                    handlerClass = namedHandlerClasses.values().iterator().next();
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
            initializeHandlerClass(handlerClass);
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
}
