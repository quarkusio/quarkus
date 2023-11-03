package io.quarkus.amazon.lambda.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.amazon.lambda.runtime.handlers.S3EventInputReader;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Used for Amazon Lambda java runtime
 *
 */
@Recorder
public class AmazonLambdaRecorder {

    private static final Logger log = Logger.getLogger(AmazonLambdaRecorder.class);

    private static Class<? extends RequestHandler<?, ?>> handlerClass;
    static Class<? extends RequestStreamHandler> streamHandlerClass;
    private static BeanContainer beanContainer;
    private static LambdaInputReader objectReader;
    private static LambdaOutputWriter objectWriter;
    protected static Set<Class<?>> expectedExceptionClasses;

    private final LambdaConfig config;

    public AmazonLambdaRecorder(LambdaConfig config) {
        this.config = config;
    }

    public void setStreamHandlerClass(Class<? extends RequestStreamHandler> handler) {
        streamHandlerClass = handler;
    }

    static void initializeHandlerClass(Class<? extends RequestHandler<?, ?>> handler) {
        handlerClass = handler;
        ObjectMapper objectMapper = AmazonLambdaMapperRecorder.objectMapper;
        Method handlerMethod = discoverHandlerMethod(handlerClass);
        Class<?> parameterType = handlerMethod.getParameterTypes()[0];
        if (parameterType.equals(S3Event.class)) {
            objectReader = new S3EventInputReader(objectMapper);
        } else {
            objectReader = new JacksonInputReader(objectMapper.readerFor(parameterType));
        }
        objectWriter = new JacksonOutputWriter(objectMapper.writerFor(handlerMethod.getReturnType()));
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

    private static Method discoverHandlerMethod(Class<? extends RequestHandler<?, ?>> handlerClass) {
        final Method[] methods = handlerClass.getMethods();
        Method method = null;
        for (int i = 0; i < methods.length && method == null; i++) {
            if (methods[i].getName().equals("handleRequest")) {
                if (methods[i].getParameterCount() == 2) {
                    final Class<?>[] types = methods[i].getParameterTypes();
                    if (!types[0].equals(Object.class)) {
                        method = methods[i];
                    }
                }
            }
        }
        if (method == null && methods.length > 0) {
            method = methods[0];
        }
        if (method == null) {
            throw new RuntimeException("Unable to find a method which handles request on handler class " + handlerClass);
        }
        return method;
    }

    public void chooseHandlerClass(List<Class<? extends RequestHandler<?, ?>>> unnamedHandlerClasses,
            Map<String, Class<? extends RequestHandler<?, ?>>> namedHandlerClasses,
            List<Class<? extends RequestStreamHandler>> unnamedStreamHandlerClasses,
            Map<String, Class<? extends RequestStreamHandler>> namedStreamHandlerClasses) {

        Class<? extends RequestHandler<?, ?>> handlerClass = null;
        Class<? extends RequestStreamHandler> handlerStreamClass = null;
        if (config.handler.isPresent()) {
            handlerClass = namedHandlerClasses.get(config.handler.get());
            handlerStreamClass = namedStreamHandlerClasses.get(config.handler.get());

            if (handlerClass == null && handlerStreamClass == null) {
                String errorMessage = "Unable to find handler class with name " + config.handler.get()
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
