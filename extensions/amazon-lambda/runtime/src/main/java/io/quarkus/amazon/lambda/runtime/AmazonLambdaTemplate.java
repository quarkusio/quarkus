package io.quarkus.amazon.lambda.runtime;

import java.lang.reflect.Method;
import java.util.Objects;

import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Template;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;

@Template
public class AmazonLambdaTemplate {

    public InstanceFactory<AmazonLambdaServlet> lambdaServletInstanceFactory(Class<? extends RequestHandler> handlerClass,
            BeanContainer beanContainer) {
        BeanContainer.Factory<? extends RequestHandler> factory = beanContainer.instanceFactory(handlerClass);
        Class<?> paramType = discoverParameterTypes(handlerClass);
        Objects.requireNonNull(paramType, "Unable to discover parameter type");
        return new InstanceFactory<AmazonLambdaServlet>() {
            @Override
            public InstanceHandle<AmazonLambdaServlet> createInstance() throws InstantiationException {
                BeanContainer.Instance<? extends RequestHandler> instance = factory.create();
                AmazonLambdaServlet servlet = new AmazonLambdaServlet(instance, paramType);
                return new InstanceHandle<AmazonLambdaServlet>() {
                    @Override
                    public AmazonLambdaServlet getInstance() {
                        return servlet;
                    }

                    @Override
                    public void release() {
                        instance.close();
                    }
                };
            }
        };
    }

    private static Class<?> discoverParameterTypes(Class<? extends RequestHandler> handlerClass) {
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
        return method.getParameterTypes()[0];
    }
}
