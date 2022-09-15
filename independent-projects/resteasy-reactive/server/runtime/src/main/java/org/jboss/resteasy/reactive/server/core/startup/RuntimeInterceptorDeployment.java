package org.jboss.resteasy.reactive.server.core.startup;

import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.WriterInterceptor;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;
import org.jboss.resteasy.reactive.common.model.HasPriority;
import org.jboss.resteasy.reactive.common.model.ResourceDynamicFeature;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptor;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.server.core.DeploymentInfo;
import org.jboss.resteasy.reactive.server.handlers.InterceptorHandler;
import org.jboss.resteasy.reactive.server.handlers.ResourceRequestFilterHandler;
import org.jboss.resteasy.reactive.server.handlers.ResourceResponseFilterHandler;
import org.jboss.resteasy.reactive.server.jaxrs.DynamicFeatureContext;
import org.jboss.resteasy.reactive.server.model.DynamicFeatures;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.spi.BeanFactory;

/**
 * Class that encapsulates the startup interceptor logic
 */
public class RuntimeInterceptorDeployment {

    private static final LinkedHashMap<ResourceInterceptor, ReaderInterceptor> EMPTY_INTERCEPTOR_MAP = new LinkedHashMap<>();

    private final Map<ResourceInterceptor<ContainerRequestFilter>, ContainerRequestFilter> globalRequestInterceptorsMap;
    private final Map<ResourceInterceptor<ContainerResponseFilter>, ContainerResponseFilter> globalResponseInterceptorsMap;
    private final Map<ResourceInterceptor<ContainerRequestFilter>, ContainerRequestFilter> nameRequestInterceptorsMap;
    private final Map<ResourceInterceptor<ContainerResponseFilter>, ContainerResponseFilter> nameResponseInterceptorsMap;
    private final Map<ResourceInterceptor<ReaderInterceptor>, ReaderInterceptor> globalReaderInterceptorsMap;
    private final Map<ResourceInterceptor<WriterInterceptor>, WriterInterceptor> globalWriterInterceptorsMap;
    private final Map<ResourceInterceptor<ReaderInterceptor>, ReaderInterceptor> nameReaderInterceptorsMap;
    private final Map<ResourceInterceptor<WriterInterceptor>, WriterInterceptor> nameWriterInterceptorsMap;
    private final Map<ResourceInterceptor<ContainerRequestFilter>, ContainerRequestFilter> preMatchContainerRequestFilters;
    private final List<ResourceRequestFilterHandler> globalRequestInterceptorHandlers;
    private final List<ResourceResponseFilterHandler> globalResponseInterceptorHandlers;
    private final InterceptorHandler globalInterceptorHandler;
    private final DeploymentInfo info;
    private final Consumer<Closeable> closeTaskHandler;
    private final ConfigurationImpl configurationImpl;

    public RuntimeInterceptorDeployment(DeploymentInfo info, ConfigurationImpl configurationImpl,
            Consumer<Closeable> closeTaskHandler) {
        this.info = info;
        this.configurationImpl = configurationImpl;
        this.closeTaskHandler = closeTaskHandler;
        ResourceInterceptors interceptors = info.getInterceptors();
        globalRequestInterceptorsMap = createInterceptorInstances(
                interceptors.getContainerRequestFilters().getGlobalResourceInterceptors(), closeTaskHandler);

        globalResponseInterceptorsMap = createInterceptorInstances(
                interceptors.getContainerResponseFilters().getGlobalResourceInterceptors(), closeTaskHandler);

        nameRequestInterceptorsMap = createInterceptorInstances(
                interceptors.getContainerRequestFilters().getNameResourceInterceptors(), closeTaskHandler);

        nameResponseInterceptorsMap = createInterceptorInstances(
                interceptors.getContainerResponseFilters().getNameResourceInterceptors(), closeTaskHandler);

        globalReaderInterceptorsMap = createInterceptorInstances(
                interceptors.getReaderInterceptors().getGlobalResourceInterceptors(), closeTaskHandler);

        globalWriterInterceptorsMap = createInterceptorInstances(
                interceptors.getWriterInterceptors().getGlobalResourceInterceptors(), closeTaskHandler);

        nameReaderInterceptorsMap = createInterceptorInstances(
                interceptors.getReaderInterceptors().getNameResourceInterceptors(), closeTaskHandler);

        nameWriterInterceptorsMap = createInterceptorInstances(
                interceptors.getWriterInterceptors().getNameResourceInterceptors(), closeTaskHandler);

        preMatchContainerRequestFilters = createInterceptorInstances(
                interceptors.getContainerRequestFilters().getPreMatchInterceptors(), closeTaskHandler);

        Collection<ContainerResponseFilter> responseFilters = globalResponseInterceptorsMap.values();
        globalResponseInterceptorHandlers = new ArrayList<>(responseFilters.size());
        for (ContainerResponseFilter responseFilter : responseFilters) {
            globalResponseInterceptorHandlers.add(new ResourceResponseFilterHandler(responseFilter));
        }
        globalRequestInterceptorHandlers = new ArrayList<>(globalRequestInterceptorsMap.size());
        for (Map.Entry<ResourceInterceptor<ContainerRequestFilter>, ContainerRequestFilter> entry : globalRequestInterceptorsMap
                .entrySet()) {
            globalRequestInterceptorHandlers
                    .add(new ResourceRequestFilterHandler(entry.getValue(), false, entry.getKey().isNonBlockingRequired(),
                            entry.getKey().isReadBody()));
        }

        InterceptorHandler globalInterceptorHandler = null;
        if (!globalReaderInterceptorsMap.isEmpty() ||
                !globalWriterInterceptorsMap.isEmpty()) {
            WriterInterceptor[] writers = null;
            ReaderInterceptor[] readers = null;
            if (!globalReaderInterceptorsMap.isEmpty()) {
                readers = new ReaderInterceptor[globalReaderInterceptorsMap.size()];
                int idx = 0;
                for (ReaderInterceptor i : globalReaderInterceptorsMap.values()) {
                    readers[idx++] = i;
                }
            }
            if (!globalWriterInterceptorsMap.isEmpty()) {
                writers = new WriterInterceptor[globalWriterInterceptorsMap.size()];
                int idx = 0;
                for (WriterInterceptor i : globalWriterInterceptorsMap.values()) {
                    writers[idx++] = i;
                }
            }
            globalInterceptorHandler = new InterceptorHandler(writers, readers);
        }
        this.globalInterceptorHandler = globalInterceptorHandler;
    }

    public InterceptorHandler getGlobalInterceptorHandler() {
        return globalInterceptorHandler;
    }

    public List<ResourceRequestFilterHandler> getGlobalRequestInterceptorHandlers() {
        return globalRequestInterceptorHandlers;
    }

    public List<ResourceResponseFilterHandler> getGlobalResponseInterceptorHandlers() {
        return globalResponseInterceptorHandlers;
    }

    public Map<ResourceInterceptor<ContainerRequestFilter>, ContainerRequestFilter> getPreMatchContainerRequestFilters() {
        return preMatchContainerRequestFilters;
    }

    private <T> LinkedHashMap<ResourceInterceptor<T>, T> createInterceptorInstances(
            List<ResourceInterceptor<T>> interceptors, Consumer<Closeable> closeTaskHandler) {

        if (interceptors.isEmpty()) {
            return (LinkedHashMap) EMPTY_INTERCEPTOR_MAP;
        }

        LinkedHashMap<ResourceInterceptor<T>, T> result = new LinkedHashMap<>();
        List<BeanFactory.BeanInstance<T>> responseBeanInstances = new ArrayList<>(interceptors.size());
        Collections.sort(interceptors);
        for (ResourceInterceptor<T> interceptor : interceptors) {
            BeanFactory.BeanInstance<T> beanInstance = interceptor.getFactory().createInstance();
            responseBeanInstances.add(beanInstance);
            T containerResponseFilter = beanInstance.getInstance();
            result.put(interceptor, containerResponseFilter);
        }
        closeTaskHandler.accept(new BeanFactory.BeanInstance.ClosingTask<>(responseBeanInstances));
        return result;
    }

    public MethodInterceptorContext forMethod(ResourceMethod method, ResteasyReactiveResourceInfo lazyMethod) {
        return new MethodInterceptorContext(method, lazyMethod);
    }

    <T> TreeMap<ResourceInterceptor<T>, T> buildInterceptorMap(
            Map<ResourceInterceptor<T>, T> globalInterceptorsMap,
            Map<ResourceInterceptor<T>, T> nameInterceptorsMap,
            Map<ResourceInterceptor<T>, T> methodSpecificInterceptorsMap, ResourceMethod method, boolean reversed) {
        TreeMap<ResourceInterceptor<T>, T> interceptorsToUse = new TreeMap<>(HasPriority.TreeMapComparator.INSTANCE);
        interceptorsToUse.putAll(globalInterceptorsMap);
        interceptorsToUse.putAll(methodSpecificInterceptorsMap);
        for (ResourceInterceptor<T> nameInterceptor : nameInterceptorsMap.keySet()) {
            // in order to the interceptor to be used, the method needs to have all the "qualifiers" that the interceptor has
            if (method.getNameBindingNames().containsAll(nameInterceptor.getNameBindingNames())) {
                interceptorsToUse.put(nameInterceptor, nameInterceptorsMap.get(nameInterceptor));
            }
        }
        return interceptorsToUse;
    }

    public class MethodInterceptorContext {
        private final ResourceMethod method;
        final Map<ResourceInterceptor<ContainerRequestFilter>, ContainerRequestFilter> methodSpecificRequestInterceptorsMap;
        final Map<ResourceInterceptor<ContainerResponseFilter>, ContainerResponseFilter> methodSpecificResponseInterceptorsMap;
        final Map<ResourceInterceptor<ReaderInterceptor>, ReaderInterceptor> methodSpecificReaderInterceptorsMap;
        final Map<ResourceInterceptor<WriterInterceptor>, WriterInterceptor> methodSpecificWriterInterceptorsMap;

        MethodInterceptorContext(ResourceMethod method, ResteasyReactiveResourceInfo lazyMethod) {
            this.method = method;
            Map<ResourceInterceptor<ContainerRequestFilter>, ContainerRequestFilter> methodSpecificRequestInterceptorsMap = Collections
                    .emptyMap();
            Map<ResourceInterceptor<ContainerResponseFilter>, ContainerResponseFilter> methodSpecificResponseInterceptorsMap = Collections
                    .emptyMap();
            Map<ResourceInterceptor<ReaderInterceptor>, ReaderInterceptor> methodSpecificReaderInterceptorsMap = Collections
                    .emptyMap();
            Map<ResourceInterceptor<WriterInterceptor>, WriterInterceptor> methodSpecificWriterInterceptorsMap = Collections
                    .emptyMap();
            DynamicFeatures dynamicFeatures = info.getDynamicFeatures();
            boolean dynamicFeaturesExist = !dynamicFeatures.getResourceDynamicFeatures().isEmpty();

            if (dynamicFeaturesExist) {
                // we'll basically just use this as a way to capture the registering of filters
                // in the global fields
                ResourceInterceptors dynamicallyConfiguredInterceptors = new ResourceInterceptors();

                DynamicFeatureContext context = new DynamicFeatureContext(
                        dynamicallyConfiguredInterceptors, configurationImpl, info.getFactoryCreator());
                for (ResourceDynamicFeature resourceDynamicFeature : dynamicFeatures.getResourceDynamicFeatures()) {
                    DynamicFeature feature = resourceDynamicFeature.getFactory().createInstance().getInstance();
                    feature.configure(lazyMethod, context);
                }
                dynamicallyConfiguredInterceptors.sort();

                if (!dynamicallyConfiguredInterceptors.getContainerRequestFilters().getGlobalResourceInterceptors()
                        .isEmpty()) {
                    methodSpecificRequestInterceptorsMap = createInterceptorInstances(
                            dynamicallyConfiguredInterceptors.getContainerRequestFilters().getGlobalResourceInterceptors(),
                            closeTaskHandler);
                }
                if (!dynamicallyConfiguredInterceptors.getContainerResponseFilters().getGlobalResourceInterceptors()
                        .isEmpty()) {
                    methodSpecificResponseInterceptorsMap = createInterceptorInstances(
                            dynamicallyConfiguredInterceptors.getContainerResponseFilters().getGlobalResourceInterceptors(),
                            closeTaskHandler);
                }
                if (!dynamicallyConfiguredInterceptors.getReaderInterceptors().getGlobalResourceInterceptors().isEmpty()) {
                    methodSpecificReaderInterceptorsMap = createInterceptorInstances(
                            dynamicallyConfiguredInterceptors.getReaderInterceptors().getGlobalResourceInterceptors(),
                            closeTaskHandler);
                }
                if (!dynamicallyConfiguredInterceptors.getWriterInterceptors().getGlobalResourceInterceptors().isEmpty()) {
                    methodSpecificWriterInterceptorsMap = createInterceptorInstances(
                            dynamicallyConfiguredInterceptors.getWriterInterceptors().getGlobalResourceInterceptors(),
                            closeTaskHandler);
                }
            }
            this.methodSpecificReaderInterceptorsMap = methodSpecificReaderInterceptorsMap;
            this.methodSpecificRequestInterceptorsMap = methodSpecificRequestInterceptorsMap;
            this.methodSpecificWriterInterceptorsMap = methodSpecificWriterInterceptorsMap;
            this.methodSpecificResponseInterceptorsMap = methodSpecificResponseInterceptorsMap;
        }

        public List<ServerRestHandler> setupResponseFilterHandler() {
            List<ServerRestHandler> responseFilterHandlers = new ArrayList<>();
            // according to the spec, global request filters apply everywhere
            // and named request filters only apply to methods with exactly matching "qualifiers"
            if (method.getNameBindingNames().isEmpty() && methodSpecificResponseInterceptorsMap.isEmpty()) {
                if (!globalResponseInterceptorHandlers.isEmpty()) {
                    responseFilterHandlers.addAll(globalResponseInterceptorHandlers);
                }
            } else if (nameResponseInterceptorsMap.isEmpty() && methodSpecificResponseInterceptorsMap.isEmpty()) {
                // in this case there are no filters that match the qualifiers, so let's just reuse the global handler
                if (!globalResponseInterceptorHandlers.isEmpty()) {
                    responseFilterHandlers.addAll(globalResponseInterceptorHandlers);
                }
            } else {
                TreeMap<ResourceInterceptor<ContainerResponseFilter>, ContainerResponseFilter> interceptorsToUse = buildInterceptorMap(
                        globalResponseInterceptorsMap, nameResponseInterceptorsMap, methodSpecificResponseInterceptorsMap,
                        method,
                        true);
                for (Map.Entry<ResourceInterceptor<ContainerResponseFilter>, ContainerResponseFilter> entry : interceptorsToUse
                        .entrySet()) {
                    responseFilterHandlers.add(new ResourceResponseFilterHandler(entry.getValue()));
                }
            }
            return responseFilterHandlers;
        }

        public ServerRestHandler setupInterceptorHandler() {
            List<ServerRestHandler> handlers = new ArrayList<>();
            if (method.getNameBindingNames().isEmpty() && methodSpecificReaderInterceptorsMap.isEmpty()
                    && methodSpecificWriterInterceptorsMap.isEmpty()) {
                return globalInterceptorHandler;
            } else if (nameReaderInterceptorsMap.isEmpty() && nameWriterInterceptorsMap.isEmpty()
                    && methodSpecificReaderInterceptorsMap.isEmpty() && methodSpecificWriterInterceptorsMap.isEmpty()) {
                // in this case there are no filters that match the qualifiers, so let's just reuse the global handler
                return globalInterceptorHandler;
            } else {
                TreeMap<ResourceInterceptor<ReaderInterceptor>, ReaderInterceptor> readerInterceptorsToUse = buildInterceptorMap(
                        globalReaderInterceptorsMap, nameReaderInterceptorsMap, methodSpecificReaderInterceptorsMap, method,
                        false);
                TreeMap<ResourceInterceptor<WriterInterceptor>, WriterInterceptor> writerInterceptorsToUse = buildInterceptorMap(
                        globalWriterInterceptorsMap, nameWriterInterceptorsMap, methodSpecificWriterInterceptorsMap, method,
                        false);
                WriterInterceptor[] writers = null;
                ReaderInterceptor[] readers = null;
                if (!readerInterceptorsToUse.isEmpty()) {
                    readers = new ReaderInterceptor[readerInterceptorsToUse.size()];
                    int idx = 0;
                    for (ReaderInterceptor i : readerInterceptorsToUse.values()) {
                        readers[idx++] = i;
                    }
                }
                if (!writerInterceptorsToUse.isEmpty()) {
                    writers = new WriterInterceptor[writerInterceptorsToUse.size()];
                    int idx = 0;
                    for (WriterInterceptor i : writerInterceptorsToUse.values()) {
                        writers[idx++] = i;
                    }
                }
                return new InterceptorHandler(writers, readers);
            }
        }

        public List<ResourceRequestFilterHandler> setupRequestFilterHandler() {
            List<ResourceRequestFilterHandler> handlers = new ArrayList<>();
            // according to the spec, global request filters apply everywhere
            // and named request filters only apply to methods with exactly matching "qualifiers"
            if (method.getNameBindingNames().isEmpty() && methodSpecificRequestInterceptorsMap.isEmpty()) {
                if (!globalRequestInterceptorHandlers.isEmpty()) {
                    handlers.addAll(globalRequestInterceptorHandlers);
                }
            } else if (nameRequestInterceptorsMap.isEmpty() && methodSpecificRequestInterceptorsMap.isEmpty()) {
                // in this case there are no filters that match the qualifiers, so let's just reuse the global handler
                if (!globalRequestInterceptorHandlers.isEmpty()) {
                    handlers.addAll(globalRequestInterceptorHandlers);
                }
            } else {
                TreeMap<ResourceInterceptor<ContainerRequestFilter>, ContainerRequestFilter> interceptorsToUse = buildInterceptorMap(
                        globalRequestInterceptorsMap, nameRequestInterceptorsMap, methodSpecificRequestInterceptorsMap, method,
                        false);
                // because named filters are different for each Resource method,
                // we need to make sure that the final set of filters do not alternate the threading model
                // from blocking to non-blocking
                validateRequestFilterThreadModel(interceptorsToUse.keySet());
                for (Map.Entry<ResourceInterceptor<ContainerRequestFilter>, ContainerRequestFilter> entry : interceptorsToUse
                        .entrySet()) {
                    handlers.add(
                            new ResourceRequestFilterHandler(entry.getValue(), false, entry.getKey().isNonBlockingRequired(),
                                    entry.getKey().isNonBlockingRequired()));
                }
            }
            return handlers;
        }

        public boolean hasWriterInterceptors() {
            if (method.getNameBindingNames().isEmpty() && methodSpecificReaderInterceptorsMap.isEmpty()
                    && methodSpecificWriterInterceptorsMap.isEmpty()) {
                if (globalInterceptorHandler != null) {
                    return globalInterceptorHandler.hasWriterInterceptors();
                }
            } else if (nameReaderInterceptorsMap.isEmpty() && nameWriterInterceptorsMap.isEmpty()
                    && methodSpecificReaderInterceptorsMap.isEmpty() && methodSpecificWriterInterceptorsMap.isEmpty()) {
                // in this case there are no filters that match the qualifiers, so let's just reuse the global handler
                if (globalInterceptorHandler != null) {
                    return globalInterceptorHandler.hasWriterInterceptors();
                }
            } else {
                // this is not optimal at all, but this method is only used for return types of AsyncFile so limited impact
                TreeMap<ResourceInterceptor<WriterInterceptor>, WriterInterceptor> writerInterceptorsToUse = buildInterceptorMap(
                        globalWriterInterceptorsMap, nameWriterInterceptorsMap, methodSpecificWriterInterceptorsMap, method,
                        false);
                return !writerInterceptorsToUse.isEmpty();
            }
            return false;
        }
    }

    /**
     * Validates that any {@code ContainerRequestFilter} that has {@code nonBlockingRequired} set, comes before any other filter
     */
    public static void validateRequestFilterThreadModel(
            Collection<ResourceInterceptor<ContainerRequestFilter>> requestFilters) {
        boolean unsetNonBlockingInterceptorFound = false;
        for (ResourceInterceptor<ContainerRequestFilter> filter : requestFilters) {
            if (filter.isNonBlockingRequired()) {
                if (unsetNonBlockingInterceptorFound) {
                    throw new RuntimeException(
                            "ContainerRequestFilters that are marked as '@NonBlocking' must be executed before any other filters.");
                }
            } else {
                unsetNonBlockingInterceptorFound = true;
            }
        }
    }

}
