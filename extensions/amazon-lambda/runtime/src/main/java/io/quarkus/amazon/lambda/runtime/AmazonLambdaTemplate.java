package io.quarkus.amazon.lambda.runtime;

import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.Application;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Template;

@Template
public class AmazonLambdaTemplate {

    private static final Logger log = Logger.getLogger(AmazonLambdaTemplate.class);

    protected static final String QUARKUS_INTERNAL_AWS_LAMBDA_TEST_API = "quarkus-internal.aws-lambda.test-api";

    public void start(Class<? extends RequestHandler> handlerClass,
            ShutdownContext context,
            RuntimeValue<Class<?>> handlerType,
            BeanContainer beanContainer) {
        RequestHandler handler = beanContainer.instance(handlerClass);

        final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        AtomicBoolean running = new AtomicBoolean(true);
        ObjectReader objectReader = mapper.readerFor(handlerType.getValue());
        ObjectReader cognitoIdReader = mapper.readerFor(CognitoIdentity.class);
        ObjectReader clientCtxReader = mapper.readerFor(ClientContext.class);

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
                            "http://" + runtimeApi() + "/2018-06-01/runtime/invocation/next");
                    while (running.get()) {

                        HttpURLConnection requestConnection = (HttpURLConnection) requestUrl.openConnection();
                        try {
                            String requestId = requestConnection.getHeaderField("Lambda-Runtime-Aws-Request-Id");
                            Object response;
                            try {
                                Object val = objectReader.readValue(requestConnection.getInputStream());
                                response = handler.handleRequest(val,
                                        new AmazonLambdaContext(requestConnection, cognitoIdReader, clientCtxReader));
                            } catch (Exception e) {
                                log.error("Failed to run lambda", e);
                                FunctionError fe = new FunctionError(e.getClass().getName(), e.getMessage());
                                URL responseUrl = new URL(
                                        "http://" + runtimeApi() + "/2018-06-01/runtime/invocation/"
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
                                    "http://" + runtimeApi() + "/2018-06-01/runtime/invocation/"
                                            + requestId + "/response");

                            HttpURLConnection responseConnection = (HttpURLConnection) responseUrl.openConnection();
                            responseConnection.setDoOutput(true);
                            responseConnection.setRequestMethod("POST");
                            mapper.writeValue(responseConnection.getOutputStream(), response);
                            while (responseConnection.getInputStream().read() != -1) {

                            }
                        } catch (Exception e) {
                            log.error("Error running lambda", e);
                            Application app = Application.currentApplication();
                            if (app != null) {
                                app.stop();
                            }
                            return;
                        } finally {
                            requestConnection.getInputStream().close();
                        }

                    }

                } catch (Exception e) {
                    try {
                        log.error("Lambda init error", e);
                        URL errorUrl = new URL(
                                "http://" + runtimeApi() + "/2018-06-01/runtime/init/error");
                        HttpURLConnection responseConnection = (HttpURLConnection) errorUrl.openConnection();
                        responseConnection.setDoOutput(true);
                        responseConnection.setRequestMethod("POST");
                        FunctionError fe = new FunctionError(e.getClass().getName(), e.getMessage());
                        mapper.writeValue(responseConnection.getOutputStream(), fe);
                        while (responseConnection.getInputStream().read() != -1) {

                        }
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

    private String runtimeApi() {
        String testApi = System.getProperty(QUARKUS_INTERNAL_AWS_LAMBDA_TEST_API);
        if (testApi != null) {
            return testApi;
        }
        return System.getenv("AWS_LAMBDA_RUNTIME_API");
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
