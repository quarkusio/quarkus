package io.quarkus.amazon.lambda.runtime;

import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Template;

@Template
public class AmazonLambdaTemplate {

    public void start(Class<? extends RequestHandler> handlerClass,
            ShutdownContext context,
            RuntimeValue<Class<?>> handlerType,
            BeanContainer beanContainer) {
        RequestHandler handler = beanContainer.instance(handlerClass);

        final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        AtomicBoolean running = new AtomicBoolean(true);
        ObjectReader objectReader = mapper.readerFor(handlerType.getValue());
        context.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                running.set(false);
            }
        });

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    URL requestUrl = new URL(
                            "http://" + System.getenv("AWS_LAMBDA_RUNTIME_API") + "/2018-06-01/runtime/invocation/next");
                    while (running.get()) {

                        HttpURLConnection requestConnection = (HttpURLConnection) requestUrl.openConnection();
                        try {
                            String requestId = requestConnection.getHeaderField("Lambda-Runtime-Aws-Request-Id");
                            Object response;
                            try {
                                Object val = objectReader.readValue(requestConnection.getInputStream());
                                response = handler.handleRequest(val, null);
                            } catch (Exception e) {
                                FunctionError fe = new FunctionError(e.getClass().getName(), e.getMessage());
                                URL responseUrl = new URL(
                                        "http://" + System.getenv("AWS_LAMBDA_RUNTIME_API") + "/2018-06-01/runtime/invocation/"
                                                + requestId + "/error");

                                HttpURLConnection responseConnection = (HttpURLConnection) responseUrl.openConnection();
                                responseConnection.setDoOutput(true);
                                responseConnection.setRequestMethod("POST");
                                mapper.writeValue(responseConnection.getOutputStream(), fe);
                                while (responseConnection.getInputStream().read() != -1) {

                                }

                                continue;
                            }

                            URL responseUrl = new URL(
                                    "http://" + System.getenv("AWS_LAMBDA_RUNTIME_API") + "/2018-06-01/runtime/invocation/"
                                            + requestId + "/response");

                            HttpURLConnection responseConnection = (HttpURLConnection) responseUrl.openConnection();
                            responseConnection.setDoOutput(true);
                            responseConnection.setRequestMethod("POST");
                            mapper.writeValue(responseConnection.getOutputStream(), response);
                            while (responseConnection.getInputStream().read() != -1) {

                            }

                        } finally {
                            requestConnection.getInputStream().close();
                        }

                    }

                } catch (Exception e) {
                    try {
                        URL errorUrl = new URL(
                                "http://" + System.getenv("AWS_LAMBDA_RUNTIME_API") + "/runtime/init/error");
                        HttpURLConnection responseConnection = (HttpURLConnection) errorUrl.openConnection();
                        responseConnection.setDoOutput(true);
                        responseConnection.setRequestMethod("POST");
                        FunctionError fe = new FunctionError(e.getClass().getName(), e.getMessage());
                        mapper.writeValue(responseConnection.getOutputStream(), fe);
                        while (responseConnection.getInputStream().read() != -1) {

                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        System.exit(1);
                    }
                }
            }
        }, "Lambda Thread");
        t.start();

    }

    public RuntimeValue<Class<?>> discoverParameterTypes(Class<? extends RequestHandler> handlerClass) {
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
        return new RuntimeValue<>(method.getParameterTypes()[0]);
    }

}
