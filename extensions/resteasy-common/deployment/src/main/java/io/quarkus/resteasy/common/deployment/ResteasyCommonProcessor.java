package io.quarkus.resteasy.common.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.AnnotationValue.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;
import org.jboss.resteasy.core.MediaTypeMap;
import org.jboss.resteasy.plugins.interceptors.AcceptEncodingGZIPFilter;
import org.jboss.resteasy.plugins.interceptors.GZIPDecodingInterceptor;
import org.jboss.resteasy.plugins.interceptors.GZIPEncodingInterceptor;
import org.jboss.resteasy.plugins.providers.StringTextStar;
import org.jboss.resteasy.plugins.providers.sse.SseConstants;
import org.jboss.resteasy.spi.InjectorFactory;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ProxyUnwrapperBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.resteasy.common.runtime.ResteasyInjectorFactoryRecorder;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.MemorySize;

public class ResteasyCommonProcessor {
    private static final Logger LOGGER = Logger.getLogger(ResteasyCommonProcessor.class.getName());

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
    public static final class ResteasyCommonConfig {
        /**
         * Enable gzip support for REST
         */
        public ResteasyCommonConfigGzip gzip;
    }

    @ConfigGroup
    public static final class ResteasyCommonConfigGzip {
        /**
         * If gzip is enabled
         */
        @ConfigItem
        public boolean enabled;
        /**
         * Maximum deflated file bytes size
         * <p>
         * If the limit is exceeded, Resteasy will return Response
         * with status 413("Request Entity Too Large")
         */
        @ConfigItem(defaultValue = "10M")
        public MemorySize maxInput;
    }

    @BuildStep
    void setupGzipProviders(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        // If GZIP support is enabled, enable it
        if (resteasyCommonConfig.gzip.enabled) {
            providers.produce(new ResteasyJaxrsProviderBuildItem(AcceptEncodingGZIPFilter.class.getName()));
            providers.produce(new ResteasyJaxrsProviderBuildItem(GZIPDecodingInterceptor.class.getName()));
            providers.produce(new ResteasyJaxrsProviderBuildItem(GZIPEncodingInterceptor.class.getName()));
        }
    }

    @Record(STATIC_INIT)
    @BuildStep
    ResteasyInjectionReadyBuildItem setupResteasyInjection(List<ProxyUnwrapperBuildItem> proxyUnwrappers,
            BeanContainerBuildItem beanContainerBuildItem,
            ResteasyInjectorFactoryRecorder recorder) {
        List<Function<Object, Object>> unwrappers = new ArrayList<>();
        for (ProxyUnwrapperBuildItem i : proxyUnwrappers) {
            unwrappers.add(i.getUnwrapper());
        }
        RuntimeValue<InjectorFactory> injectorFactory = recorder.setup(beanContainerBuildItem.getValue(), unwrappers);
        return new ResteasyInjectionReadyBuildItem(injectorFactory);
    }

    @BuildStep
    JaxrsProvidersToRegisterBuildItem setupProviders(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem indexBuildItem,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            List<ResteasyJaxrsProviderBuildItem> contributedProviderBuildItems, Capabilities capabilities) throws Exception {

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

        if (!capabilities.isCapabilityPresent(Capabilities.RESTEASY_JSON_EXTENSION)) {

            boolean needJsonSupport = restJsonSupportNeeded(indexBuildItem, ResteasyDotNames.CONSUMES)
                    || restJsonSupportNeeded(indexBuildItem, ResteasyDotNames.PRODUCES)
                    || restJsonSupportNeeded(indexBuildItem, ResteasyDotNames.RESTEASY_SSE_ELEMENT_TYPE);
            if (needJsonSupport) {
                LOGGER.warn(
                        "Quarkus detected the need of REST JSON support but you have not provided the necessary JSON " +
                                "extension for this. You can visit https://quarkus.io/guides/rest-json for more " +
                                "information on how to set one.");
            }
        }

        // we add a couple of default providers
        providersToRegister.add(StringTextStar.class.getName());
        providersToRegister.addAll(categorizedWriters.getPossible(MediaType.APPLICATION_JSON_TYPE));

        IndexView index = indexBuildItem.getIndex();
        IndexView beansIndex = beanArchiveIndexBuildItem.getIndex();

        // find the providers declared in our services
        boolean useBuiltinProviders = collectDeclaredProviders(providersToRegister, categorizedReaders, categorizedWriters,
                categorizedContextResolvers, index, beansIndex);

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

    private boolean restJsonSupportNeeded(CombinedIndexBuildItem indexBuildItem, DotName mediaTypeAnnotation) {
        for (AnnotationInstance annotationInstance : indexBuildItem.getIndex().getAnnotations(mediaTypeAnnotation)) {
            final AnnotationValue annotationValue = annotationInstance.value();
            if (annotationValue == null) {
                continue;
            }

            List<String> mediaTypes = Collections.emptyList();
            if (annotationValue.kind() == Kind.ARRAY) {
                mediaTypes = Arrays.asList(annotationValue.asStringArray());
            } else if (annotationValue.kind() == Kind.STRING) {
                mediaTypes = Collections.singletonList(annotationValue.asString());
            }
            return mediaTypes.contains(MediaType.APPLICATION_JSON)
                    || mediaTypes.contains(MediaType.APPLICATION_JSON_PATCH_JSON);
        }

        return false;
    }

    public static void categorizeProviders(Set<String> availableProviders, MediaTypeMap<String> categorizedReaders,
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
            MediaTypeMap<String> categorizedContextResolvers, IndexView... indexes) {

        for (IndexView index : indexes) {
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
                            categorizedContextResolvers, methodTarget, ResteasyDotNames.CONSUMES,
                            providerDiscoverer.noConsumesDefaultsToAll())) {
                        return true;
                    }
                    if (collectDeclaredProvidersForMethodAndMediaTypeAnnotation(providersToRegister,
                            categorizedContextResolvers, methodTarget, ResteasyDotNames.PRODUCES,
                            providerDiscoverer.noProducesDefaultsToAll())) {
                        return true;
                    }
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
                            classAnnotation, methodTarget)) {
                        return true;
                    }
                    return false;
                }
            }
            return defaultsToAll;
        }
        if (collectDeclaredProvidersForMediaTypeAnnotationInstance(providersToRegister, categorizedProviders,
                mediaTypeAnnotationInstance, methodTarget)) {
            return true;
        }

        return false;
    }

    private static boolean collectDeclaredProvidersForMediaTypeAnnotationInstance(Set<String> providersToRegister,
            MediaTypeMap<String> categorizedProviders,
            AnnotationInstance mediaTypeAnnotationInstance,
            MethodInfo targetMethod) {
        for (String media : mediaTypeAnnotationInstance.value().asStringArray()) {
            MediaType mediaType = MediaType.valueOf(media);
            if (MediaType.WILDCARD_TYPE.equals(mediaType)) {
                // exit early if we have the wildcard type
                return true;
            }
            providersToRegister.addAll(categorizedProviders.getPossible(mediaType));
            // additionally add any "inferred" providers based on the media type
            providersToRegister.addAll(collectInferredProviders(mediaType, categorizedProviders, targetMethod));
        }
        return false;
    }

    /**
     * Returns a collection of providers that are "inferred" based on certain rules applied to the passed
     * {@code mediaType}. Returns an empty collection if no providers were inferred.
     *
     * @param mediaType The MediaType to process
     * @param categorizedProviders Available providers that are categorized based on their media type. This map
     *        will be used to find possible providers that can be used for the passed
     *        {@code mediaType}
     * @return
     */
    private static Collection<String> collectInferredProviders(final MediaType mediaType,
            final MediaTypeMap<String> categorizedProviders, final MethodInfo targetMethod) {

        // for SERVER_SENT_EVENTS media type, we do certain things:
        // - check if the @SseElementType (RestEasy) specific annotation is specified on the target.
        //   if it is, then include a provider which can handle that element type.
        // - if no @SseElementType is present, check if the media type has the "element-type" parameter
        //   and if it does then include the provider which can handle that element-type
        // - if neither of the above specifies an element-type then we by fallback to including text/plain
        //   provider as a default
        if (matches(MediaType.SERVER_SENT_EVENTS_TYPE, mediaType)) {
            final Set<String> additionalProvidersToRegister = new HashSet<>();
            // first check for @SseElementType
            final AnnotationInstance sseElementTypeAnnInst = targetMethod
                    .annotation(ResteasyDotNames.RESTEASY_SSE_ELEMENT_TYPE);
            String elementType = null;
            if (sseElementTypeAnnInst != null) {
                elementType = sseElementTypeAnnInst.value().asString();
            } else if (mediaType.getParameters() != null
                    && mediaType.getParameters().containsKey(SseConstants.SSE_ELEMENT_MEDIA_TYPE)) {
                // fallback on the MediaType parameter
                elementType = mediaType.getParameters().get(SseConstants.SSE_ELEMENT_MEDIA_TYPE);
            }
            if (elementType != null) {
                additionalProvidersToRegister.addAll(categorizedProviders.getPossible(MediaType.valueOf(elementType)));
            } else {
                // add text/plain provider as a fallback default for SSE mediatype
                additionalProvidersToRegister.addAll(categorizedProviders.getPossible(MediaType.TEXT_PLAIN_TYPE));
            }
            return additionalProvidersToRegister;
        }
        return Collections.emptySet();
    }

    /**
     * Compares the {@link MediaType#getType() type} and the {@link MediaType#getSubtype() subtype} to see if they are
     * equal (case insensitive). If they are equal, then this method returns {@code true}, else returns {@code false}.
     * Unlike the {@link MediaType#equals(Object)}, this method doesn't take into account the {@link MediaType#getParameters()
     * parameters} during the equality check
     *
     * @param m1 one of the MediaType
     * @param m2 the other MediaType
     * @return
     */
    private static boolean matches(final MediaType m1, final MediaType m2) {
        if (m1 == null || m2 == null) {
            return false;
        }
        if (m1.getType() == null || m1.getSubtype() == null) {
            return false;
        }
        return m1.getType().equalsIgnoreCase(m2.getType()) && m1.getSubtype().equalsIgnoreCase(m2.getSubtype());
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
