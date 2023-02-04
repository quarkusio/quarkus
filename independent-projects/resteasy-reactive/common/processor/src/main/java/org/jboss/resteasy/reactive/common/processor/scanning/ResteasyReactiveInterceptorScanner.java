package org.jboss.resteasy.reactive.common.processor.scanning;

import static org.jboss.resteasy.reactive.common.model.ResourceInterceptor.FILTER_SOURCE_METHOD_METADATA_KEY;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.*;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.model.InterceptorContainer;
import org.jboss.resteasy.reactive.common.model.PreMatchInterceptorContainer;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptor;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;
import org.jboss.resteasy.reactive.common.processor.NameBindingUtil;
import org.jboss.resteasy.reactive.spi.BeanFactory;

/**
 * Scanner that is responsible for scanning for interceptor classes, such as container filters and IO interceptors
 */
public class ResteasyReactiveInterceptorScanner {

    private ResteasyReactiveInterceptorScanner() {

    }

    /**
     * Creates a fully populated resource interceptors instance, that are created via reflection.
     */
    public static ResourceInterceptors createResourceInterceptors(IndexView indexView, ApplicationScanningResult result) {
        return createResourceInterceptors(indexView, result, null);
    }

    /**
     * Creates a fully populated resource interceptors instance, that are created via the provided factory creator
     */
    public static ResourceInterceptors createResourceInterceptors(IndexView indexView, ApplicationScanningResult result,
            Function<String, BeanFactory<?>> factoryCreator) {
        ResourceInterceptors interceptors = new ResourceInterceptors();
        scanForContainerRequestFilters(interceptors, indexView, result);
        scanForIOInterceptors(interceptors, indexView, result);
        if (factoryCreator != null) {
            interceptors.initializeDefaultFactories(factoryCreator);
        }
        return interceptors;
    }

    public static void scanForContainerRequestFilters(ResourceInterceptors interceptors, IndexView index,
            ApplicationScanningResult applicationScanningResult) {
        //the quarkus version of these filters will not be in the index
        //so you need an explicit check for both

        Collection<ClassInfo> specReqFilters = new HashSet<>(index
                .getAllKnownImplementors(CONTAINER_REQUEST_FILTER));
        Collection<ClassInfo> allReqFilters = new HashSet<>(specReqFilters);
        allReqFilters.addAll(index
                .getAllKnownImplementors(RESTEASY_REACTIVE_CONTAINER_REQUEST_FILTER));
        for (var filterClass : allReqFilters) {
            var interceptor = handleDiscoveredInterceptor(applicationScanningResult, interceptors.getContainerRequestFilters(),
                    index, filterClass);
            if (interceptor == null) {
                continue;
            }
            setFilterMethodSourceForReqFilter(index, specReqFilters, filterClass, interceptor);
        }

        Collection<ClassInfo> specRespFilters = new HashSet<>(index
                .getAllKnownImplementors(CONTAINER_RESPONSE_FILTER));
        Collection<ClassInfo> allRespFilters = new HashSet<>(specRespFilters);
        allRespFilters.addAll(index
                .getAllKnownImplementors(RESTEASY_REACTIVE_CONTAINER_RESPONSE_FILTER));

        for (var filterClass : allRespFilters) {
            var interceptor = handleDiscoveredInterceptor(applicationScanningResult, interceptors.getContainerResponseFilters(),
                    index, filterClass);
            if (interceptor == null) {
                continue;
            }
            setFilterMethodSourceForRespFilter(index, specRespFilters, filterClass, interceptor);
        }
    }

    private static void setFilterMethodSourceForReqFilter(IndexView index, Collection<ClassInfo> specRequestFilters,
            ClassInfo filterClass, ResourceInterceptor<ContainerRequestFilter> interceptor) {
        boolean isSpecFilter = specRequestFilters.contains(filterClass);
        ClassInfo ci = filterClass;
        MethodInfo filterSourceMethod = null;
        do {
            for (var method : ci.methods()) {
                if (!method.name().equals("filter") || method.parametersCount() != 1) {
                    continue;
                }
                List<Type> parameterTypes = method.parameterTypes();
                if (isSpecFilter) {
                    if (parameterTypes.get(0).name().equals(CONTAINER_REQUEST_CONTEXT)) {
                        filterSourceMethod = method;
                        break;
                    }
                } else {
                    if (parameterTypes.get(0).name().equals(RESTEASY_REACTIVE_CONTAINER_REQUEST_CONTEXT)) {
                        filterSourceMethod = method;
                        break;
                    }
                }
            }
            if (filterSourceMethod != null) {
                break;
            }

            if (OBJECT.equals(ci.superName())) {
                break;
            }
            ci = index.getClassByName(ci.superName());
            if (ci == null) {
                break;
            }

        } while (true);
        if (filterSourceMethod != null) {
            interceptor.metadata = Map.of(FILTER_SOURCE_METHOD_METADATA_KEY, filterSourceMethod);
        }
    }

    private static void setFilterMethodSourceForRespFilter(IndexView index, Collection<ClassInfo> specResponseFilters,
            ClassInfo filterClass, ResourceInterceptor<ContainerResponseFilter> interceptor) {
        boolean isSpecFilter = specResponseFilters.contains(filterClass);
        ClassInfo ci = filterClass;
        MethodInfo filterSourceMethod = null;
        do {
            for (var method : ci.methods()) {
                if (!method.name().equals("filter") || method.parametersCount() != 2) {
                    continue;
                }
                List<Type> parameterTypes = method.parameterTypes();
                if (isSpecFilter) {
                    if (parameterTypes.get(0).name().equals(CONTAINER_REQUEST_CONTEXT) &&
                            parameterTypes.get(1).name().equals(CONTAINER_RESPONSE_CONTEXT)) {
                        filterSourceMethod = method;
                        break;
                    }
                } else {
                    if (parameterTypes.get(0).name().equals(RESTEASY_REACTIVE_CONTAINER_REQUEST_CONTEXT) &&
                            parameterTypes.get(1).name().equals(CONTAINER_RESPONSE_CONTEXT)) {
                        filterSourceMethod = method;
                        break;
                    }
                }
            }
            if (filterSourceMethod != null) {
                break;
            }

            if (OBJECT.equals(ci.superName())) {
                break;
            }
            ci = index.getClassByName(ci.superName());
            if (ci == null) {
                break;
            }

        } while (true);
        if (filterSourceMethod != null) {
            interceptor.metadata = Map.of(FILTER_SOURCE_METHOD_METADATA_KEY, filterSourceMethod);
        }
    }

    public static void scanForIOInterceptors(ResourceInterceptors interceptors, IndexView index,
            ApplicationScanningResult applicationScanningResult) {
        Collection<ClassInfo> readerInterceptors = index
                .getAllKnownImplementors(READER_INTERCEPTOR);
        Collection<ClassInfo> writerInterceptors = index
                .getAllKnownImplementors(WRITER_INTERCEPTOR);

        for (ClassInfo filterClass : writerInterceptors) {
            handleDiscoveredInterceptor(applicationScanningResult, interceptors.getWriterInterceptors(), index, filterClass);
        }
        for (ClassInfo filterClass : readerInterceptors) {
            handleDiscoveredInterceptor(applicationScanningResult, interceptors.getReaderInterceptors(), index, filterClass);
        }
    }

    private static <T> ResourceInterceptor<T> handleDiscoveredInterceptor(
            ApplicationScanningResult applicationResultBuildItem, InterceptorContainer<T> interceptorContainer, IndexView index,
            ClassInfo filterClass) {
        if (Modifier.isAbstract(filterClass.flags())) {
            return null;
        }
        ApplicationScanningResult.KeepProviderResult keepProviderResult = applicationResultBuildItem.keepProvider(filterClass);
        if (keepProviderResult != ApplicationScanningResult.KeepProviderResult.DISCARD) {
            ResourceInterceptor<T> interceptor = interceptorContainer.create();
            interceptor.setClassName(filterClass.name().toString());
            interceptor.setNameBindingNames(NameBindingUtil.nameBindingNames(index, filterClass));
            AnnotationInstance priorityInstance = filterClass.classAnnotation(PRIORITY);
            if (priorityInstance != null) {
                interceptor.setPriority(priorityInstance.value().asInt());
            }
            AnnotationInstance nonBlockingInstance = filterClass.classAnnotation(NON_BLOCKING);
            if (nonBlockingInstance != null) {
                interceptor.setNonBlockingRequired(true);
            }
            if (interceptorContainer instanceof PreMatchInterceptorContainer
                    && filterClass.classAnnotation(PRE_MATCHING) != null) {
                ((PreMatchInterceptorContainer<T>) interceptorContainer).addPreMatchInterceptor(interceptor);
            } else {
                Set<String> nameBindingNames = interceptor.getNameBindingNames();
                if (nameBindingNames.isEmpty()
                        || namePresent(nameBindingNames, applicationResultBuildItem.getGlobalNameBindings())) {
                    interceptorContainer.addGlobalRequestInterceptor(interceptor);
                } else {
                    interceptorContainer.addNameRequestInterceptor(interceptor);
                }
            }
            return interceptor;
        }

        return null;
    }

    private static boolean namePresent(Set<String> nameBindingNames, Set<String> globalNameBindings) {
        for (String i : globalNameBindings) {
            if (nameBindingNames.contains(i)) {
                return true;
            }
        }
        return false;
    }
}
