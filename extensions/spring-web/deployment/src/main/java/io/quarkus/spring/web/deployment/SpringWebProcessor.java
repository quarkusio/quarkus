package io.quarkus.spring.web.deployment;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Providers;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.resteasy.core.MediaTypeMap;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.metadata.SpringResourceBuilder;
import org.jboss.resteasy.web.ResponseEntityReturnTypeHandler;
import org.jboss.resteasy.web.ResponseStatusBuiltResponseProcessor;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.resteasy.common.deployment.ResteasyCommonProcessor;
import io.quarkus.resteasy.common.deployment.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.server.common.deployment.AdditionalJaxRsResourceDefiningAnnotationBuildItem;
import io.quarkus.resteasy.server.common.deployment.AdditionalJaxRsResourceMethodAnnotationsBuildItem;
import io.quarkus.resteasy.server.common.deployment.AdditionalJaxRsResourceMethodParamAnnotations;
import io.quarkus.undertow.deployment.BlacklistedServletContainerInitializerBuildItem;
import io.quarkus.undertow.deployment.ServletInitParamBuildItem;

public class SpringWebProcessor {

    private static final DotName REST_CONTROLLER_ANNOTATION = DotName
            .createSimple("org.springframework.web.bind.annotation.RestController");

    private static final List<DotName> MAPPING_CLASSES = Arrays.asList(
            DotName.createSimple("org.springframework.web.bind.annotation.RequestMapping"),
            DotName.createSimple("org.springframework.web.bind.annotation.GetMapping"),
            DotName.createSimple("org.springframework.web.bind.annotation.PostMapping"),
            DotName.createSimple("org.springframework.web.bind.annotation.PutMapping"),
            DotName.createSimple("org.springframework.web.bind.annotation.DeleteMapping"),
            DotName.createSimple("org.springframework.web.bind.annotation.PatchMapping"));

    @BuildStep
    public BlacklistedServletContainerInitializerBuildItem blacklistSpringServlet() {
        return new BlacklistedServletContainerInitializerBuildItem("org.springframework.web.SpringServletContainerInitializer");
    }

    @BuildStep
    public AdditionalJaxRsResourceDefiningAnnotationBuildItem additionalJaxRsResourceDefiningAnnotation() {
        return new AdditionalJaxRsResourceDefiningAnnotationBuildItem(REST_CONTROLLER_ANNOTATION);
    }

    @BuildStep
    public AdditionalJaxRsResourceMethodAnnotationsBuildItem additionalJaxRsResourceMethodAnnotationsBuildItem() {
        return new AdditionalJaxRsResourceMethodAnnotationsBuildItem(MAPPING_CLASSES);
    }

    @BuildStep
    public AdditionalJaxRsResourceMethodParamAnnotations additionalJaxRsResourceMethodParamAnnotations() {
        return new AdditionalJaxRsResourceMethodParamAnnotations(
                Arrays.asList(DotName.createSimple("org.springframework.web.bind.annotation.RequestParam"),
                        DotName.createSimple("org.springframework.web.bind.annotation.PathVariable"),
                        DotName.createSimple("org.springframework.web.bind.annotation.RequestBody"),
                        DotName.createSimple("org.springframework.web.bind.annotation.MatrixVariable"),
                        DotName.createSimple("org.springframework.web.bind.annotation.RequestHeader"),
                        DotName.createSimple("org.springframework.web.bind.annotation.CookieValue")));
    }

    @BuildStep
    public void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(REST_CONTROLLER_ANNOTATION, BuiltinScope.SINGLETON.getName()));
    }

    @BuildStep
    public void process(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ServletInitParamBuildItem> initParamProducer) {

        final IndexView index = beanArchiveIndexBuildItem.getIndex();
        final Collection<AnnotationInstance> annotations = index.getAnnotations(REST_CONTROLLER_ANNOTATION);
        if (annotations.isEmpty()) {
            return;
        }

        final Set<String> classNames = new HashSet<>();
        for (AnnotationInstance annotation : annotations) {
            classNames.add(annotation.target().asClass().toString());
        }

        initParamProducer.produce(
                new ServletInitParamBuildItem(
                        ResteasyContextParameters.RESTEASY_SCANNED_RESOURCE_CLASSES_WITH_BUILDER,
                        SpringResourceBuilder.class.getName() + ":" + String.join(",", classNames)));

        // TODO perhaps only register if we detect the use of ResponseEntity ?
        initParamProducer.produce(
                new ServletInitParamBuildItem(
                        ResteasyContextParameters.RESTEASY_ADDITIONAL_RESPONSE_TYPE_HANDLERS,
                        ResponseEntityReturnTypeHandler.class.getName()));

        // TODO perhaps only register if we detect the use of @ResponseStatus ?
        initParamProducer.produce(
                new ServletInitParamBuildItem(
                        ResteasyContextParameters.RESTEASY_BUILT_RESPONSE_PROCESSORS,
                        ResponseStatusBuiltResponseProcessor.class.getName()));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, SpringResourceBuilder.class.getName()));
    }

    @BuildStep
    public void registerProviders(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BuildProducer<ResteasyJaxrsProviderBuildItem> providersProducer) throws IOException {

        //TODO only read this information once since it is exactly the same in ResteasyCommonProcessor#setupProviders
        final Set<String> availableProviders = ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                "META-INF/services/" + Providers.class.getName());

        final MediaTypeMap<String> categorizedReaders = new MediaTypeMap<>();
        final MediaTypeMap<String> categorizedWriters = new MediaTypeMap<>();
        final MediaTypeMap<String> categorizedContextResolvers = new MediaTypeMap<>();
        final Set<String> otherProviders = new HashSet<>();

        ResteasyCommonProcessor.categorizeProviders(availableProviders, categorizedReaders, categorizedWriters,
                categorizedContextResolvers,
                otherProviders);

        boolean useAllAvailable = false;
        Set<String> providersToRegister = new HashSet<>();

        OUTER: for (DotName mappingClass : MAPPING_CLASSES) {
            final Collection<AnnotationInstance> instances = beanArchiveIndexBuildItem.getIndex().getAnnotations(mappingClass);
            for (AnnotationInstance instance : instances) {
                if (collectProviders(providersToRegister, categorizedWriters, instance, "produces")) {
                    useAllAvailable = true;
                    break OUTER;
                }

                if (collectProviders(providersToRegister, categorizedReaders, instance, "consumes")) {
                    useAllAvailable = true;
                    break OUTER;
                }
            }
        }

        if (useAllAvailable) {
            providersToRegister = availableProviders;
        }

        for (String provider : providersToRegister) {
            providersProducer.produce(new ResteasyJaxrsProviderBuildItem(provider));
        }
    }

    private boolean collectProviders(Set<String> providersToRegister, MediaTypeMap<String> categorizedProviders,
            AnnotationInstance instance, String annotationValueName) {
        final AnnotationValue producesValue = instance.value(annotationValueName);
        if (producesValue != null) {
            for (String value : producesValue.asStringArray()) {
                MediaType mediaType = MediaType.valueOf(value);
                if (MediaType.WILDCARD_TYPE.equals(mediaType)) {
                    // exit early if we have the wildcard type
                    return true;
                }
                providersToRegister.addAll(categorizedProviders.getPossible(mediaType));
            }
        }
        return false;
    }
}
