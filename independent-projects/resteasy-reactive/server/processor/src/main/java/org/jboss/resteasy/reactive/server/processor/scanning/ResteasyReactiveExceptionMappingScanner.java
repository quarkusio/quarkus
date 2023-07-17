package org.jboss.resteasy.reactive.server.processor.scanning;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import jakarta.ws.rs.Priorities;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.model.ResourceExceptionMapper;
import org.jboss.resteasy.reactive.common.processor.JandexUtil;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.scanning.ApplicationScanningResult;
import org.jboss.resteasy.reactive.server.core.ExceptionMapping;
import org.jboss.resteasy.reactive.spi.BeanFactory;

/**
 * Class that is responsible for scanning for exception mappers
 */
public class ResteasyReactiveExceptionMappingScanner {

    /**
     * Creates a fully populated exception mapper instance, that are created via reflection.
     */
    public static ExceptionMapping createExceptionMappers(IndexView indexView, ApplicationScanningResult result) {
        return scanForExceptionMappers(indexView, result);
    }

    /**
     * Creates a fully populated exception mapper instance, that are created via the provided factory creator
     */
    public static ExceptionMapping createExceptionMappers(IndexView indexView, ApplicationScanningResult result,
            Function<String, BeanFactory<?>> factoryCreator) {
        ExceptionMapping ret = scanForExceptionMappers(indexView, result);
        ret.initializeDefaultFactories(factoryCreator);
        return ret;
    }

    public static ExceptionMapping scanForExceptionMappers(IndexView index, ApplicationScanningResult result) {

        ExceptionMapping exceptionMapping = new ExceptionMapping();
        Collection<ClassInfo> exceptionMappers = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.EXCEPTION_MAPPER);
        for (ClassInfo mapperClass : exceptionMappers) {
            ApplicationScanningResult.KeepProviderResult keepProviderResult = result
                    .keepProvider(mapperClass);
            if (keepProviderResult != ApplicationScanningResult.KeepProviderResult.DISCARD) {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(mapperClass.name(),
                        ResteasyReactiveDotNames.EXCEPTION_MAPPER,
                        index);
                DotName handledExceptionDotName = typeParameters.get(0).name();
                AnnotationInstance priorityInstance = mapperClass.declaredAnnotation(ResteasyReactiveDotNames.PRIORITY);
                int priority = Priorities.USER;
                if (priorityInstance != null) {
                    priority = priorityInstance.value().asInt();
                }
                ResourceExceptionMapper mapper = new ResourceExceptionMapper<>();
                mapper.setPriority(priority);
                mapper.setClassName(mapperClass.name().toString());
                exceptionMapping.addExceptionMapper(handledExceptionDotName.toString(), mapper);
            }
        }
        return exceptionMapping;
    }
}
