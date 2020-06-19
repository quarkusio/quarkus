package io.quarkus.qrs.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.ws.rs.core.MediaType;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.qrs.runtime.core.ArcEndpointFactory;
import io.quarkus.qrs.runtime.core.RestHandler;
import io.quarkus.qrs.runtime.handlers.InstanceHandler;
import io.quarkus.qrs.runtime.handlers.InvocationHandler;
import io.quarkus.qrs.runtime.handlers.QrsInitialHandler;
import io.quarkus.qrs.runtime.handlers.ResponseHandler;
import io.quarkus.qrs.runtime.mapping.RequestMapper;
import io.quarkus.qrs.runtime.mapping.RuntimeResource;
import io.quarkus.qrs.runtime.mapping.URITemplate;
import io.quarkus.qrs.runtime.model.ResourceClass;
import io.quarkus.qrs.runtime.model.ResourceMethod;
import io.quarkus.qrs.runtime.spi.EndpointFactory;
import io.quarkus.qrs.runtime.spi.EndpointInvoker;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class QrsRecorder {

    public EndpointFactory factory(String targetClass, BeanContainer beanContainer) {
        try {
            return new ArcEndpointFactory(Class.forName(targetClass, false, Thread.currentThread().getContextClassLoader()),
                    beanContainer);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot load JAX-RS endpoint class", e);
        }
    }

    public Supplier<EndpointInvoker> invoker(String baseName) {
        return new Supplier<EndpointInvoker>() {
            @Override
            public EndpointInvoker get() {
                try {
                    return (EndpointInvoker) Class.forName(baseName, false, Thread.currentThread().getContextClassLoader())
                            .newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Unable to generate endpoint invoker", e);
                }

            }
        };
    }

    public Handler<RoutingContext> handler(List<ResourceClass> resourceClasses) {
        List<RequestMapper.RequestPath<RuntimeResource>> templates = new ArrayList<>();
        for (ResourceClass clazz : resourceClasses) {
            for (ResourceMethod method : clazz.getMethods()) {
                List<RestHandler> handlers = new ArrayList<>();
                EndpointInvoker invoker = method.getInvoker().get();
                handlers.add(new InstanceHandler(clazz.getFactory()));
                handlers.add(new InvocationHandler(invoker));
                handlers.add(new ResponseHandler());
                Class<?>[] parameterTypes = new Class[method.getParameters().length];
                for (int i = 0; i < method.getParameters().length; ++i) {
                    parameterTypes[i] = loadClass(method.getParameters()[i]);
                }
                RuntimeResource resource = new RuntimeResource(method.getMethod(), new URITemplate(method.getPath()),
                        method.getProduces() == null ? null : MediaType.valueOf(method.getProduces()), method.getConsumes() == null ? null : MediaType.valueOf(method.getConsumes()), invoker,
                        clazz.getFactory(), handlers.toArray(new RestHandler[0]), method.getName(), parameterTypes,
                        loadClass(method.getReturnType()));
                templates.add(new RequestMapper.RequestPath<>(resource.getPath(), resource));
            }
        }
        return new QrsInitialHandler(new RequestMapper<>(templates));
    }

    private static Class<?> loadClass(String name) {
        try {
            return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
