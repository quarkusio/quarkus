package org.jboss.resteasy.reactive.common.processor.scanning;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.common.model.InterceptorContainer;
import org.jboss.resteasy.reactive.common.model.PreMatchInterceptorContainer;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptor;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;
import org.jboss.resteasy.reactive.common.processor.NameBindingUtil;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.reflection.ReflectionBeanFactoryCreator;
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
        return createResourceInterceptors(indexView, result, new ReflectionBeanFactoryCreator());
    }

    /**
     * Creates a fully populated resource interceptors instance, that are created via the provided factory creator
     */
    public static ResourceInterceptors createResourceInterceptors(IndexView indexView, ApplicationScanningResult result,
            Function<String, BeanFactory<?>> factoryCreator) {
        ResourceInterceptors interceptors = new ResourceInterceptors();
        scanForInterceptors(interceptors, indexView, result);
        scanForIOInterceptors(interceptors, indexView, result);
        interceptors.initializeDefaultFactories(factoryCreator);
        return interceptors;
    }

    public static void scanForInterceptors(ResourceInterceptors interceptors, IndexView index,
            ApplicationScanningResult applicationScanningResult) {
        //the quarkus version of these filters will not be in the index
        //so you need an explicit check for both
        Collection<ClassInfo> containerResponseFilters = new HashSet<>(index
                .getAllKnownImplementors(ResteasyReactiveDotNames.CONTAINER_RESPONSE_FILTER));
        containerResponseFilters.addAll(index
                .getAllKnownImplementors(ResteasyReactiveDotNames.QUARKUS_REST_CONTAINER_RESPONSE_FILTER));
        Collection<ClassInfo> containerRequestFilters = new HashSet<>(index
                .getAllKnownImplementors(ResteasyReactiveDotNames.CONTAINER_REQUEST_FILTER));
        containerRequestFilters.addAll(index
                .getAllKnownImplementors(ResteasyReactiveDotNames.QUARKUS_REST_CONTAINER_REQUEST_FILTER));
        for (ClassInfo filterClass : containerRequestFilters) {
            handleDiscoveredInterceptor(applicationScanningResult, interceptors.getContainerRequestFilters(),
                    index, filterClass);
        }
        for (ClassInfo filterClass : containerResponseFilters) {
            handleDiscoveredInterceptor(applicationScanningResult, interceptors.getContainerResponseFilters(),
                    index, filterClass);
        }
    }

    public static void scanForIOInterceptors(ResourceInterceptors interceptors, IndexView index,
            ApplicationScanningResult applicationScanningResult) {
        Collection<ClassInfo> readerInterceptors = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.READER_INTERCEPTOR);
        Collection<ClassInfo> writerInterceptors = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.WRITER_INTERCEPTOR);

        for (ClassInfo filterClass : writerInterceptors) {
            handleDiscoveredInterceptor(applicationScanningResult, interceptors.getWriterInterceptors(), index, filterClass);
        }
        for (ClassInfo filterClass : readerInterceptors) {
            handleDiscoveredInterceptor(applicationScanningResult, interceptors.getReaderInterceptors(), index, filterClass);
        }
    }

    private static <T> void handleDiscoveredInterceptor(
            ApplicationScanningResult applicationResultBuildItem, InterceptorContainer<T> interceptorContainer, IndexView index,
            ClassInfo filterClass) {
        if (Modifier.isAbstract(filterClass.flags())) {
            return;
        }
        ApplicationScanningResult.KeepProviderResult keepProviderResult = applicationResultBuildItem.keepProvider(filterClass);
        if (keepProviderResult != ApplicationScanningResult.KeepProviderResult.DISCARD) {
            ResourceInterceptor<T> interceptor = interceptorContainer.create();
            interceptor.setClassName(filterClass.name().toString());
            interceptor.setNameBindingNames(NameBindingUtil.nameBindingNames(index, filterClass));
            AnnotationInstance priorityInstance = filterClass.classAnnotation(ResteasyReactiveDotNames.PRIORITY);
            if (priorityInstance != null) {
                interceptor.setPriority(priorityInstance.value().asInt());
            }
            AnnotationInstance nonBlockingInstance = filterClass.classAnnotation(ResteasyReactiveDotNames.NON_BLOCKING);
            if (nonBlockingInstance != null) {
                interceptor.setNonBlockingRequired(true);
            }
            if (interceptorContainer instanceof PreMatchInterceptorContainer
                    && filterClass.classAnnotation(ResteasyReactiveDotNames.PRE_MATCHING) != null) {
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
        }
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
