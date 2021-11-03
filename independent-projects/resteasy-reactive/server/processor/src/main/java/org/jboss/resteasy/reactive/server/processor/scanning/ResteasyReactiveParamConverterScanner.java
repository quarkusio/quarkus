package org.jboss.resteasy.reactive.server.processor.scanning;

import java.util.Collection;
import java.util.function.Function;
import javax.ws.rs.Priorities;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.common.model.ResourceParamConverterProvider;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.scanning.ApplicationScanningResult;
import org.jboss.resteasy.reactive.common.reflection.ReflectionBeanFactoryCreator;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;
import org.jboss.resteasy.reactive.spi.BeanFactory;

/**
 * Class that is responsible for scanning for param converters
 */
public class ResteasyReactiveParamConverterScanner {

    /**
     * Creates a fully populated param converter instance, that are created via reflection.
     */
    public static ParamConverterProviders createParamConverters(IndexView indexView, ApplicationScanningResult result) {
        return createParamConverters(indexView, result, new ReflectionBeanFactoryCreator());
    }

    /**
     * Creates a fully populated param converter instance, that are created via the provided factory creator
     */
    public static ParamConverterProviders createParamConverters(IndexView indexView, ApplicationScanningResult result,
            Function<String, BeanFactory<?>> factoryCreator) {
        ParamConverterProviders ret = scanForParamConverters(indexView, result);
        ret.initializeDefaultFactories(factoryCreator);
        return ret;
    }

    public static ParamConverterProviders scanForParamConverters(IndexView index, ApplicationScanningResult result) {

        Collection<ClassInfo> paramConverterProviders = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.PARAM_CONVERTER_PROVIDER);
        ParamConverterProviders providers = new ParamConverterProviders();
        for (ClassInfo converterClass : paramConverterProviders) {
            ApplicationScanningResult.KeepProviderResult keepProviderResult = result.keepProvider(converterClass);
            if (keepProviderResult != ApplicationScanningResult.KeepProviderResult.DISCARD) {
                AnnotationInstance priorityInstance = converterClass.classAnnotation(ResteasyReactiveDotNames.PRIORITY);
                int priority = priorityInstance != null ? priorityInstance.value().asInt() : Priorities.USER;
                ResourceParamConverterProvider provider = new ResourceParamConverterProvider();
                provider.setPriority(priority);
                provider.setClassName(converterClass.name().toString());
                providers.addParamConverterProviders(provider);
            }
        }
        return providers;
    }
}
