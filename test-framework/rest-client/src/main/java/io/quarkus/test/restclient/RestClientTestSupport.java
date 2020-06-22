package io.quarkus.test.restclient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Singleton;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.client.jaxrs.internal.ClientWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.proxy.ClientInvoker;
import org.jboss.resteasy.client.jaxrs.internal.proxy.ClientProxy;
import org.jboss.resteasy.client.jaxrs.internal.proxy.MethodInvoker;
import org.jboss.resteasy.microprofile.client.ProxyInvocationHandler;
import org.jboss.resteasy.microprofile.client.impl.MpClientWebTarget;
import org.jboss.resteasy.specimpl.ResteasyUriBuilderImpl;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.ClientProxyUnwrapper;

/**
 * Provides a way for tests to change the baseURI of a rest-client
 */
public class RestClientTestSupport {

    private static final Map<Class<?>, Object> restClientClassToObject = new ConcurrentHashMap<>();
    private static final Map<Object, RestClientContext> restClientObjectToContext = new ConcurrentHashMap<>();

    // needed for unwrapping @ApplicationScoped beans
    private static final ClientProxyUnwrapper unwrapper = new ClientProxyUnwrapper();

    /**
     * Given a class that is a rest-client interface (that has been made a Quarkus bean in the usual ways)
     * sets the a new baseURI.
     */
    public static void setBaseURI(Class<?> restClientClass, URI newBaseURI) {
        verifyBean(restClientClass);
        Object restClient = restClientClassToObject.computeIfAbsent(restClientClass,
                (k -> unwrapper.apply(Arc.container().instance(k, RestClient.LITERAL).get())));

        Map<Field, UriBuilder> originalUriBuilders = new HashMap<>();
        updateUriBuilder(restClient, restClientClass, c -> {
            ResteasyUriBuilderImpl currentURIBuilder = c.getCurrentURIBuilderForMethod();
            UriBuilder newUriBuilder = UriBuilder.fromUri(newBaseURI);

            // currentURIBuilder.getPath() contains the entire path of the request
            // meaning it has info from the baseURL, the path defined on the class
            // and the path defined on the method.
            // We need to make sure that any path of the original baseURL is removed
            // before adding the method specific path part
            String path = currentURIBuilder.getPath() == null ? "" : currentURIBuilder.getPath();
            if (!path.isEmpty()) {
                newUriBuilder.path(path.replace(c.getBasePath(), ""));
            }

            c.useUriBuilder(newUriBuilder);
            originalUriBuilders.put(c.getUriBuilderField(), currentURIBuilder);
        });

        restClientObjectToContext.put(restClient, new RestClientContext(restClientClass, originalUriBuilders));
    }

    /**
     * Restore the original baseURI that was associated with the {@code restClientClass} interface
     *
     * This method is called automatically by Quarkus after each test and for the time being isn't exposed
     */
    private static void resetURL(Class<?> restClientClass) {
        Object restClient = restClientClassToObject.get(restClientClass);
        if (restClient == null) {
            throw new IllegalStateException("Unable to reset URL for rest-client class '" + restClientClass.getName()
                    + "'. Please make sure the URL had been previously set");
        }
        updateUriBuilder(restClient, restClientClass, c -> {
            Map<Field, UriBuilder> originalUriBuilders = restClientObjectToContext.get(restClient).getOriginalUriBuilders();
            UriBuilder originalUriBuilder = originalUriBuilders.get(c.getUriBuilderField());
            c.useUriBuilder(originalUriBuilder);
        });
        restClientObjectToContext.remove(restClient);
    }

    /**
     * Get a collection of rest-client classes that have currently been updated
     *
     * This method is called automatically by Quarkus after each test and for the time being isn't exposed
     */
    private static Collection<Class<?>> activeUpdatedRestClients() {
        if (restClientObjectToContext.isEmpty()) {
            return Collections.emptyList();
        }
        List<Class<?>> classes = new ArrayList<>(restClientObjectToContext.values().size());
        Collection<RestClientContext> contexts = restClientObjectToContext.values();
        for (RestClientContext context : contexts) {
            classes.add(context.getRestClientClass());
        }
        return classes;
    }

    /**
     * This makes use of more reflection that we would like, but currently
     * there are no hooks into the internals of the rest-client that would allow
     * us to change URIs
     */
    private static void updateUriBuilder(Object restClient, Class<?> restClientClass,
            Consumer<RestClientMethodContext> consumer) {
        try {
            ProxyInvocationHandler mpProxyInvocationHandler = (ProxyInvocationHandler) Proxy
                    .getInvocationHandler(restClient);

            Field targetField = ProxyInvocationHandler.class.getDeclaredField("target");
            targetField.setAccessible(true);
            Object targetObject = targetField.get(mpProxyInvocationHandler);

            ClientProxy clientProxy = (ClientProxy) Proxy.getInvocationHandler(targetObject);

            Field clientProxyTargetField = ClientProxy.class.getDeclaredField("target");
            clientProxyTargetField.setAccessible(true);
            WebTarget classWebTarget = (MpClientWebTarget) clientProxyTargetField.get(clientProxy);
            ResteasyUriBuilderImpl classUriBuilder = (ResteasyUriBuilderImpl) classWebTarget.getUriBuilder();
            String basePath = getBasePath(restClientClass, classUriBuilder);

            Field methodMapField = ClientProxy.class.getDeclaredField("methodMap");
            methodMapField.setAccessible(true);
            Map<Method, MethodInvoker> methodMap = (Map<Method, MethodInvoker>) methodMapField.get(clientProxy);
            for (Map.Entry<Method, MethodInvoker> entry : methodMap.entrySet()) {
                ClientInvoker clientInvoker = (ClientInvoker) entry.getValue();

                Field webTargetField = ClientInvoker.class.getDeclaredField("webTarget");
                webTargetField.setAccessible(true);

                ClientWebTarget clientWebTarget = (MpClientWebTarget) webTargetField.get(clientInvoker);
                Field uriBuilderField = ClientWebTarget.class.getDeclaredField("uriBuilder");
                uriBuilderField.setAccessible(true);
                ResteasyUriBuilderImpl uriBuilder = (ResteasyUriBuilderImpl) uriBuilderField.get(clientWebTarget);

                consumer.accept(new RestClientMethodContext(uriBuilderField, uriBuilder,
                        clientWebTarget, basePath));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return The base path of the original baseURL. Never returns {@code null}
     */
    private static String getBasePath(Class<?> restClientClass, ResteasyUriBuilderImpl classUriBuilder) {
        ResteasyUriBuilderImpl uriBuilderFromClass = (ResteasyUriBuilderImpl) UriBuilder.fromResource(restClientClass);
        if (classUriBuilder.getPath() == null) {
            return "";
        }
        return classUriBuilder.getPath().replace(uriBuilderFromClass.getPath() == null ? "" : uriBuilderFromClass.getPath(),
                "");
    }

    private static void verifyBean(Class<?> restClientClass) {
        Set<Bean<?>> beans = Arc.container().beanManager().getBeans(restClientClass, RestClient.LITERAL);
        if (beans.isEmpty()) {
            throw new IllegalArgumentException("No RestClient bean of type '" + restClientClass + "' exists");
        }

        Bean<?> bean = beans.iterator().next();
        if (!bean.getScope().equals(ApplicationScoped.class) && !bean.getScope().equals(Singleton.class)) {
            throw new IllegalStateException(
                    "RestClient beans with the default scope or a scope other than '@ApplicationScoped' and '@Singleton' cannot be updated. To be able to update the base URL, consider using one of these scoped");
        }
    }

    private static class RestClientMethodContext {
        private final Field uriBuilderField;
        private final ResteasyUriBuilderImpl currentURIBuilderForMethod;
        private final ClientWebTarget clientWebTarget;
        private final String basePath;

        public RestClientMethodContext(Field uriBuilderField, ResteasyUriBuilderImpl currentUriBuilder,
                ClientWebTarget clientWebTarget, String basePath) {
            this.uriBuilderField = uriBuilderField;
            this.currentURIBuilderForMethod = currentUriBuilder;
            this.clientWebTarget = clientWebTarget;
            this.basePath = basePath;
        }

        public Field getUriBuilderField() {
            return uriBuilderField;
        }

        public ResteasyUriBuilderImpl getCurrentURIBuilderForMethod() {
            return currentURIBuilderForMethod;
        }

        public String getBasePath() {
            return basePath;
        }

        public void useUriBuilder(UriBuilder uriBuilder) {
            try {
                uriBuilderField.set(clientWebTarget, uriBuilder);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class RestClientContext {
        private final Class<?> restClientClass;
        private final Map<Field, UriBuilder> originalUriBuilders;

        public RestClientContext(Class<?> restClientClass, Map<Field, UriBuilder> originalUriBuilders) {
            this.restClientClass = restClientClass;
            this.originalUriBuilders = originalUriBuilders;
        }

        public Class<?> getRestClientClass() {
            return restClientClass;
        }

        public Map<Field, UriBuilder> getOriginalUriBuilders() {
            return originalUriBuilders;
        }
    }
}
