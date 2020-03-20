package io.quarkus.amazon.lambda.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.Application;
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
    private static Class<? extends RequestStreamHandler> streamHandlerClass;
    private static BeanContainer beanContainer;
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static ObjectReader objectReader;
    private static ObjectWriter objectWriter;

    public void setStreamHandlerClass(Class<? extends RequestStreamHandler> handler, BeanContainer container) {
        streamHandlerClass = handler;
        beanContainer = container;
        AmazonLambdaRecorder.objectMapper = getObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                .registerModule(new JodaModule());

        Method handlerMethod = discoverStreamHandlerMethod(streamHandlerClass);
    }

    public void setHandlerClass(Class<? extends RequestHandler<?, ?>> handler, BeanContainer container) {
        handlerClass = handler;
        beanContainer = container;
        AmazonLambdaRecorder.objectMapper = getObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                .registerModule(new JodaModule());

        Method handlerMethod = discoverHandlerMethod(handlerClass);
        objectReader = objectMapper.readerFor(handlerMethod.getParameterTypes()[0]);
        objectWriter = objectMapper.writerFor(handlerMethod.getReturnType());
    }

    private ObjectMapper getObjectMapper() {
        InstanceHandle<ObjectMapper> instance = Arc.container().instance(ObjectMapper.class);
        if (instance.isAvailable()) {
            return instance.get().copy();
        }
        return new ObjectMapper();
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
            RequestStreamHandler handler = beanContainer.instance(streamHandlerClass);
            handler.handleRequest(inputStream, outputStream, context);
        } else {
            Object request = objectReader.readValue(inputStream);
            RequestHandler handler = beanContainer.instance(handlerClass);
            Object response = handler.handleRequest(request, context);
            objectWriter.writeValue(outputStream, response);
        }
    }

    private Method discoverStreamHandlerMethod(Class<? extends RequestStreamHandler> handlerClass) {
        final Method[] methods = handlerClass.getMethods();
        Method method = null;
        for (int i = 0; i < methods.length && method == null; i++) {
            if (methods[i].getName().equals("handleRequest")) {
                final Class<?>[] types = methods[i].getParameterTypes();
                if (types.length == 3 && !types[0].equals(Object.class)) {
                    method = methods[i];
                }
            }
        }
        if (method == null) {
            method = methods[0];
        }
        return method;
    }

    private Method discoverHandlerMethod(Class<? extends RequestHandler<?, ?>> handlerClass) {
        final Method[] methods = handlerClass.getMethods();
        Method method = null;
        for (int i = 0; i < methods.length && method == null; i++) {
            if (methods[i].getName().equals("handleRequest")) {
                final Class<?>[] types = methods[i].getParameterTypes();
                if (types.length == 2 && !types[0].equals(Object.class)) {
                    method = methods[i];
                }
            }
        }
        if (method == null) {
            method = methods[0];
        }
        return method;
    }

    public void chooseHandlerClass(List<Class<? extends RequestHandler<?, ?>>> unamedHandlerClasses,
            Map<String, Class<? extends RequestHandler<?, ?>>> namedHandlerClasses,
            List<Class<? extends RequestStreamHandler>> unamedStreamHandlerClasses,
            Map<String, Class<? extends RequestStreamHandler>> namedStreamHandlerClasses,
            BeanContainer container,
            LambdaConfig config) {

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
            int unnamedTotal = unamedHandlerClasses.size() + unamedStreamHandlerClasses.size();
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
                if (!unamedHandlerClasses.isEmpty()) {
                    handlerClass = unamedHandlerClasses.get(0);
                } else if (!namedHandlerClasses.isEmpty()) {
                    handlerClass = namedHandlerClasses.values().iterator().next();
                } else if (!unamedStreamHandlerClasses.isEmpty()) {
                    handlerStreamClass = unamedStreamHandlerClasses.get(0);
                } else if (!namedStreamHandlerClasses.isEmpty()) {
                    handlerStreamClass = namedStreamHandlerClasses.values().iterator().next();
                }
            }
        }

        if (handlerStreamClass != null) {
            setStreamHandlerClass(handlerStreamClass, container);
        } else {
            setHandlerClass(handlerClass, container);
        }
    }

    @SuppressWarnings("rawtypes")
    public void startPollLoop(ShutdownContext context) {
        AtomicBoolean running = new AtomicBoolean(true);

        ObjectReader cognitoIdReader = objectMapper.readerFor(CognitoIdentity.class);
        ObjectReader clientCtxReader = objectMapper.readerFor(ClientContext.class);

        context.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                running.set(false);
            }
        });
        Thread t = new Thread(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {

                try {
                    checkQuarkusBootstrapped();
                    URL requestUrl = AmazonLambdaApi.invocationNext();
                    while (running.get()) {

                        HttpURLConnection requestConnection = (HttpURLConnection) requestUrl.openConnection();
                        HttpURLConnection responseStreamConnection = null;
                        try {
                            String requestId = requestConnection.getHeaderField(AmazonLambdaApi.LAMBDA_RUNTIME_AWS_REQUEST_ID);
                            Object response = null;
                            try {
                                // todo custom runtime requires the setting of an ENV Var
                                // Should we override getenv?  If we do that,
                                // then this will never be able to process multiple requests at the same time
                                String traceId = requestConnection.getHeaderField(AmazonLambdaApi.LAMBDA_TRACE_HEADER_KEY);
                                TraceId.setTraceId(traceId);

                                if (streamHandlerClass != null) {
                                    RequestStreamHandler handler = beanContainer.instance(streamHandlerClass);
                                    responseStreamConnection = responseStream(AmazonLambdaApi.invocationResponse(requestId));
                                    handler.handleRequest(requestConnection.getInputStream(),
                                            responseStreamConnection.getOutputStream(),
                                            new AmazonLambdaContext(requestConnection, cognitoIdReader, clientCtxReader));
                                    while (responseStreamConnection.getInputStream().read() != -1) {
                                        // Read data
                                    }
                                } else {
                                    Object val = objectReader.readValue(requestConnection.getInputStream());
                                    RequestHandler handler = beanContainer.instance(handlerClass);
                                    response = handler.handleRequest(val,
                                            new AmazonLambdaContext(requestConnection, cognitoIdReader, clientCtxReader));
                                }
                            } catch (Exception e) {
                                log.error("Failed to run lambda", e);
                                postResponse(AmazonLambdaApi.invocationError(requestId),
                                        new FunctionError(e.getClass().getName(), e.getMessage()), objectMapper);

                                continue;
                            }

                            if (streamHandlerClass == null) {
                                postResponse(AmazonLambdaApi.invocationResponse(requestId), response, objectMapper);
                            }
                        } catch (Exception e) {
                            log.error("Error running lambda", e);
                            Application app = Application.currentApplication();
                            if (app != null) {
                                app.stop();
                            }
                            return;
                        } finally {
                            if (responseStreamConnection != null) {
                                responseStreamConnection.getInputStream().close();
                            }
                            requestConnection.getInputStream().close();
                        }
                    }
                } catch (Exception e) {
                    try {
                        log.error("Lambda init error", e);
                        postResponse(AmazonLambdaApi.initError(), new FunctionError(e.getClass().getName(), e.getMessage()),
                                objectMapper);
                    } catch (Exception ex) {
                        log.error("Failed to report init error", ex);
                    } finally {
                        //our main loop is done, time to shutdown
                        Application app = Application.currentApplication();
                        if (app != null) {
                            app.stop();
                        }
                    }
                }
            }
        }, "Lambda Thread");
        t.start();

    }

    private void checkQuarkusBootstrapped() {
        // todo we need a better way to do this.
        if (Application.currentApplication() == null) {
            throw new RuntimeException("Quarkus initialization error");
        }
        String[] args = {};
        Application.currentApplication().start(args);
    }

    private HttpURLConnection responseStream(URL url) throws IOException {
        HttpURLConnection responseConnection = (HttpURLConnection) url.openConnection();
        responseConnection.setDoOutput(true);
        responseConnection.setRequestMethod("POST");
        return responseConnection;
    }

    private void postResponse(URL url, Object response, ObjectMapper mapper) throws IOException {
        HttpURLConnection responseConnection = (HttpURLConnection) url.openConnection();
        responseConnection.setDoOutput(true);
        responseConnection.setRequestMethod("POST");
        mapper.writeValue(responseConnection.getOutputStream(), response);
        while (responseConnection.getInputStream().read() != -1) {
            // Read data
        }
    }

}
