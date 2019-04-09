package io.quarkus.resteasy.common.deployment;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.*;

import org.jboss.jandex.*;
import org.jboss.resteasy.core.MediaTypeMap;
import org.jboss.resteasy.plugins.interceptors.AcceptEncodingGZIPFilter;
import org.jboss.resteasy.plugins.interceptors.GZIPDecodingInterceptor;
import org.jboss.resteasy.plugins.interceptors.GZIPEncodingInterceptor;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

public class ResteasyCommonProcessor {

    private static final ProviderDiscoverer[] PROVIDER_DISCOVERERS = {
            new ProviderDiscoverer(ResteasyDotNames.GET, false, true),
            new ProviderDiscoverer(ResteasyDotNames.HEAD, false, false),
            new ProviderDiscoverer(ResteasyDotNames.DELETE, true, false),
            new ProviderDiscoverer(ResteasyDotNames.OPTIONS, false, true),
            new ProviderDiscoverer(ResteasyDotNames.PATCH, true, false),
            new ProviderDiscoverer(ResteasyDotNames.POST, true, true),
            new ProviderDiscoverer(ResteasyDotNames.PUT, true, false)
    };

    private ResteasyCommonConfig resteasyCommonConfig;

    @ConfigRoot(name = "resteasy")
    static final class ResteasyCommonConfig {
        /**
         * Enable gzip support for REST Clients.
         */
        @ConfigItem(defaultValue = "false")
        boolean enableGzip;
    }

    @BuildStep
    void setupGzipProviders(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        // If GZIP support is enabled, enable it
        if (resteasyCommonConfig.enableGzip) {
            providers.produce(new ResteasyJaxrsProviderBuildItem(AcceptEncodingGZIPFilter.class.getName()));
            providers.produce(new ResteasyJaxrsProviderBuildItem(GZIPDecodingInterceptor.class.getName()));
            providers.produce(new ResteasyJaxrsProviderBuildItem(GZIPEncodingInterceptor.class.getName()));
        }
    }

    @BuildStep
    JaxrsProvidersToRegisterBuildItem setupProviders(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem indexBuildItem,
            List<ResteasyJaxrsProviderBuildItem> contributedProviderBuildItems) throws Exception {

        Set<String> contributedProviders = new HashSet<>();
        for (ResteasyJaxrsProviderBuildItem contributedProviderBuildItem : contributedProviderBuildItems) {
            contributedProviders.add(contributedProviderBuildItem.getName());
        }
        for (AnnotationInstance i : indexBuildItem.getIndex().getAnnotations(ResteasyDotNames.PROVIDER)) {
            if (i.target().kind() == AnnotationTarget.Kind.CLASS) {
                contributedProviders.add(i.target().asClass().name().toString());
            }
        }

        Set<String> availableProviders = ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                "META-INF/services/" + Providers.class.getName());

        MediaTypeMap<String> categorizedReaders = new MediaTypeMap<>();
        MediaTypeMap<String> categorizedWriters = new MediaTypeMap<>();
        MediaTypeMap<String> categorizedContextResolvers = new MediaTypeMap<>();
        Set<String> otherProviders = new HashSet<>();

        categorizeProviders(availableProviders, categorizedReaders, categorizedWriters, categorizedContextResolvers,
                otherProviders);

        // add the other providers detected
        Set<String> providersToRegister = new HashSet<>(otherProviders);

        IndexView index = indexBuildItem.getIndex();

        // find the providers declared in our services
        boolean useBuiltinProviders = collectDeclaredProviders(providersToRegister, categorizedReaders, categorizedWriters,
                categorizedContextResolvers, index);

        if (useBuiltinProviders) {
            providersToRegister = new HashSet<>(contributedProviders);
            providersToRegister.addAll(availableProviders);
        } else {
            providersToRegister.addAll(contributedProviders);
        }

        if (providersToRegister.contains("org.jboss.resteasy.plugins.providers.jsonb.JsonBindingProvider")) {
            // This abstract one is also accessed directly via reflection
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
                    "org.jboss.resteasy.plugins.providers.jsonb.AbstractJsonBindingProvider"));
        }

        return new JaxrsProvidersToRegisterBuildItem(providersToRegister, contributedProviders, useBuiltinProviders);
    }

    private static void categorizeProviders(Set<String> availableProviders, MediaTypeMap<String> categorizedReaders,
            MediaTypeMap<String> categorizedWriters, MediaTypeMap<String> categorizedContextResolvers,
            Set<String> otherProviders) {
        for (String availableProvider : availableProviders) {
            try {
                Class<?> providerClass = Class.forName(availableProvider);
                if (MessageBodyReader.class.isAssignableFrom(providerClass)
                        || MessageBodyWriter.class.isAssignableFrom(providerClass)) {
                    if (MessageBodyReader.class.isAssignableFrom(providerClass)) {
                        Consumes consumes = providerClass.getAnnotation(Consumes.class);
                        if (consumes != null) {
                            for (String consumesMediaType : consumes.value()) {
                                categorizedReaders.add(MediaType.valueOf(consumesMediaType), providerClass.getName());
                            }
                        } else {
                            categorizedReaders.add(MediaType.WILDCARD_TYPE, providerClass.getName());
                        }
                    }
                    if (MessageBodyWriter.class.isAssignableFrom(providerClass)) {
                        Produces produces = providerClass.getAnnotation(Produces.class);
                        if (produces != null) {
                            for (String producesMediaType : produces.value()) {
                                categorizedWriters.add(MediaType.valueOf(producesMediaType), providerClass.getName());
                            }
                        } else {
                            categorizedWriters.add(MediaType.WILDCARD_TYPE, providerClass.getName());
                        }
                    }
                } else if (ContextResolver.class.isAssignableFrom(providerClass)) {
                    Produces produces = providerClass.getAnnotation(Produces.class);
                    if (produces != null) {
                        for (String producesMediaType : produces.value()) {
                            categorizedContextResolvers.add(MediaType.valueOf(producesMediaType),
                                    providerClass.getName());
                        }
                    } else {
                        categorizedContextResolvers.add(MediaType.WILDCARD_TYPE, providerClass.getName());
                    }
                } else {
                    otherProviders.add(providerClass.getName());
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }
    }

    private static boolean collectDeclaredProviders(Set<String> providersToRegister,
            MediaTypeMap<String> categorizedReaders, MediaTypeMap<String> categorizedWriters,
            MediaTypeMap<String> categorizedContextResolvers, IndexView index) {
        for (ProviderDiscoverer providerDiscoverer : PROVIDER_DISCOVERERS) {
            Collection<AnnotationInstance> getMethods = index.getAnnotations(providerDiscoverer.getMethodAnnotation());
            for (AnnotationInstance getMethod : getMethods) {
                MethodInfo methodTarget = getMethod.target().asMethod();
                if (collectDeclaredProvidersForMethodAndMediaTypeAnnotation(providersToRegister, categorizedReaders,
                        methodTarget, ResteasyDotNames.CONSUMES, providerDiscoverer.noConsumesDefaultsToAll())) {
                    return true;
                }
                if (collectDeclaredProvidersForMethodAndMediaTypeAnnotation(providersToRegister, categorizedWriters,
                        methodTarget, ResteasyDotNames.PRODUCES, providerDiscoverer.noProducesDefaultsToAll())) {
                    return true;
                }
                if (collectDeclaredProvidersForMethodAndMediaTypeAnnotation(providersToRegister,
                        categorizedContextResolvers, methodTarget, ResteasyDotNames.PRODUCES,
                        providerDiscoverer.noProducesDefaultsToAll())) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean collectDeclaredProvidersForMethodAndMediaTypeAnnotation(Set<String> providersToRegister,
            MediaTypeMap<String> categorizedProviders, MethodInfo methodTarget, DotName mediaTypeAnnotation,
            boolean defaultsToAll) {
        AnnotationInstance mediaTypeAnnotationInstance = methodTarget.annotation(mediaTypeAnnotation);
        if (mediaTypeAnnotationInstance == null) {
            // let's consider the class
            Collection<AnnotationInstance> classAnnotations = methodTarget.declaringClass().classAnnotations();
            for (AnnotationInstance classAnnotation : classAnnotations) {
                if (mediaTypeAnnotation.equals(classAnnotation.name())) {
                    if (collectDeclaredProvidersForMediaTypeAnnotationInstance(providersToRegister, categorizedProviders,
                            classAnnotation)) {
                        return true;
                    }
                    return false;
                }
            }
            return defaultsToAll;
        }
        if (collectDeclaredProvidersForMediaTypeAnnotationInstance(providersToRegister, categorizedProviders,
                mediaTypeAnnotationInstance)) {
            return true;
        }

        return false;
    }

    private static boolean collectDeclaredProvidersForMediaTypeAnnotationInstance(Set<String> providersToRegister,
            MediaTypeMap<String> categorizedProviders, AnnotationInstance mediaTypeAnnotationInstance) {
        for (String media : mediaTypeAnnotationInstance.value().asStringArray()) {
            MediaType mediaType = MediaType.valueOf(media);
            if (MediaType.WILDCARD_TYPE.equals(mediaType)) {
                // exit early if we have the wildcard type
                return true;
            }
            providersToRegister.addAll(categorizedProviders.getPossible(mediaType));
        }
        return false;
    }

    private static class ProviderDiscoverer {

        private final DotName methodAnnotation;

        private final boolean noConsumesDefaultsToAll;

        private final boolean noProducesDefaultsToAll;

        private ProviderDiscoverer(DotName methodAnnotation, boolean noConsumesDefaultsToAll,
                boolean noProducesDefaultsToAll) {
            this.methodAnnotation = methodAnnotation;
            this.noConsumesDefaultsToAll = noConsumesDefaultsToAll;
            this.noProducesDefaultsToAll = noProducesDefaultsToAll;
        }

        public DotName getMethodAnnotation() {
            return methodAnnotation;
        }

        public boolean noConsumesDefaultsToAll() {
            return noConsumesDefaultsToAll;
        }

        public boolean noProducesDefaultsToAll() {
            return noProducesDefaultsToAll;
        }
    }
}
