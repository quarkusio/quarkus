package org.jboss.resteasy.reactive.server.processor.scanning;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.model.ResourceContextResolver;
import org.jboss.resteasy.reactive.common.processor.JandexUtil;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.scanning.ApplicationScanningResult;
import org.jboss.resteasy.reactive.common.reflection.ReflectionBeanFactoryCreator;
import org.jboss.resteasy.reactive.server.model.ContextResolvers;
import org.jboss.resteasy.reactive.spi.BeanFactory;

/**
 * Class that is responsible for scanning for exception mappers
 */
public class ResteasyReactiveContextResolverScanner {

    /**
     * Creates a fully populated exception mapper instance, that are created via reflection.
     */
    public static ContextResolvers createContextResolvers(IndexView indexView, ApplicationScanningResult result) {
        return createContextResolvers(indexView, result, new ReflectionBeanFactoryCreator());
    }

    /**
     * Creates a fully populated exception mapper instance, that are created via the provided factory creator
     */
    public static ContextResolvers createContextResolvers(IndexView indexView, ApplicationScanningResult result,
            Function<String, BeanFactory<?>> factoryCreator) {
        ContextResolvers ret = scanForContextResolvers(indexView, result);
        ret.initializeDefaultFactories(factoryCreator);
        return ret;
    }

    public static ContextResolvers scanForContextResolvers(IndexView index, ApplicationScanningResult result) {

        ContextResolvers contextResolvers = new ContextResolvers();
        Collection<ClassInfo> resolvers = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.CONTEXT_RESOLVER);
        for (ClassInfo resolverClass : resolvers) {
            ApplicationScanningResult.KeepProviderResult keepProviderResult = result
                    .keepProvider(resolverClass);
            if (keepProviderResult != ApplicationScanningResult.KeepProviderResult.DISCARD) {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(resolverClass.name(),
                        ResteasyReactiveDotNames.CONTEXT_RESOLVER,
                        index);
                DotName typeParam = typeParameters.get(0).name();
                ResourceContextResolver mapper = new ResourceContextResolver();
                mapper.setClassName(resolverClass.name().toString());
                mapper.setMediaTypeStrings(getProducesMediaTypes(resolverClass));
                try {
                    Class contextType = Class.forName(typeParam.toString(), false,
                            Thread.currentThread().getContextClassLoader());
                    contextResolvers.addContextResolver(contextType, mapper);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Unable to load context type: " + typeParam);
                }
            }
        }
        return contextResolvers;
    }

    private static List<String> getProducesMediaTypes(ClassInfo classInfo) {
        AnnotationInstance produces = classInfo.classAnnotation(ResteasyReactiveDotNames.PRODUCES);
        if (produces == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(produces.value().asStringArray());
    }
}
