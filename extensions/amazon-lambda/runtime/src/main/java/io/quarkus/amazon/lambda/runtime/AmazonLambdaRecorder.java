package io.quarkus.amazon.lambda.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Used for Amazon Lambda java runtime
 *
 */
@Recorder
public class AmazonLambdaRecorder {

    private static final Logger log = Logger.getLogger(AmazonLambdaRecorder.class);

    private static Class<? extends RequestHandler<?, ?>> handlerClass;
    private static BeanContainer beanContainer;
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static ObjectReader objectReader;
    private static ObjectWriter objectWriter;

    public void setHandlerClass(Class<? extends RequestHandler<?, ?>> handler, BeanContainer container) {
        handlerClass = handler;
        beanContainer = container;
        objectMapper = new ObjectMapper();
        Method handlerMethod = discoverHandlerMethod(handlerClass);
        objectReader = objectMapper.readerFor(handlerMethod.getParameterTypes()[0]);
        objectWriter = objectMapper.writerFor(handlerMethod.getReturnType());
    }

    public static void handle(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        Object request = objectReader.readValue(inputStream);
        RequestHandler handler = beanContainer.instance(handlerClass);
        Object response = handler.handleRequest(request, context);
        objectWriter.writeValue(outputStream, response);
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

}
