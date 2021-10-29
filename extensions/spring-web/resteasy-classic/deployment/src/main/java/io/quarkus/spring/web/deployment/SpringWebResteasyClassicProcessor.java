package io.quarkus.spring.web.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Providers;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;
import org.jboss.resteasy.core.MediaTypeMap;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.metadata.SpringResourceBuilder;
import org.jboss.resteasy.spring.web.ResponseEntityFeature;
import org.jboss.resteasy.spring.web.ResponseStatusFeature;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.resteasy.common.deployment.ResteasyCommonProcessor;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.runtime.ExceptionMapperRecorder;
import io.quarkus.resteasy.runtime.NonJaxRsClassMappings;
import io.quarkus.resteasy.server.common.deployment.ResteasyDeploymentCustomizerBuildItem;
import io.quarkus.resteasy.server.common.spi.AdditionalJaxRsResourceDefiningAnnotationBuildItem;
import io.quarkus.resteasy.server.common.spi.AdditionalJaxRsResourceMethodParamAnnotations;
import io.quarkus.spring.web.runtime.ResteasyClassicResponseStatusExceptionMapper;
import io.quarkus.undertow.deployment.IgnoredServletContainerInitializerBuildItem;
import io.quarkus.undertow.deployment.ServletInitParamBuildItem;

public class SpringWebResteasyClassicProcessor {

    private static final Logger LOGGER = Logger.getLogger(SpringWebResteasyClassicProcessor.class.getName());

    private static final DotName REQUEST_MAPPING = DotName
            .createSimple("org.springframework.web.bind.annotation.RequestMapping");
    private static final DotName GET_MAPPING = DotName.createSimple("org.springframework.web.bind.annotation.GetMapping");
    private static final DotName POST_MAPPING = DotName.createSimple("org.springframework.web.bind.annotation.PostMapping");
    private static final DotName PUT_MAPPING = DotName.createSimple("org.springframework.web.bind.annotation.PutMapping");
    private static final DotName DELETE_MAPPING = DotName.createSimple("org.springframework.web.bind.annotation.DeleteMapping");
    private static final DotName PATCH_MAPPING = DotName.createSimple("org.springframework.web.bind.annotation.PatchMapping");
    private static final List<DotName> MAPPING_ANNOTATIONS = List.of(REQUEST_MAPPING, GET_MAPPING, POST_MAPPING,
            PUT_MAPPING, DELETE_MAPPING, PATCH_MAPPING);

    private static final DotName REST_CONTROLLER_ANNOTATION = DotName
            .createSimple("org.springframework.web.bind.annotation.RestController");

    private static final DotName PATH_VARIABLE = DotName.createSimple("org.springframework.web.bind.annotation.PathVariable");

    @BuildStep
    public IgnoredServletContainerInitializerBuildItem ignoreSpringServlet() {
        return new IgnoredServletContainerInitializerBuildItem("org.springframework.web.SpringServletContainerInitializer");
    }

    @BuildStep
    public AdditionalJaxRsResourceDefiningAnnotationBuildItem additionalJaxRsResourceDefiningAnnotation() {
        return new AdditionalJaxRsResourceDefiningAnnotationBuildItem(REST_CONTROLLER_ANNOTATION);
    }

    @BuildStep
    public AdditionalJaxRsResourceMethodParamAnnotations additionalJaxRsResourceMethodParamAnnotations() {
        return new AdditionalJaxRsResourceMethodParamAnnotations(
                List.of(DotName.createSimple("org.springframework.web.bind.annotation.RequestParam"),
                        PATH_VARIABLE,
                        DotName.createSimple("org.springframework.web.bind.annotation.RequestBody"),
                        DotName.createSimple("org.springframework.web.bind.annotation.MatrixVariable"),
                        DotName.createSimple("org.springframework.web.bind.annotation.RequestHeader"),
                        DotName.createSimple("org.springframework.web.bind.annotation.CookieValue")));
    }

    @BuildStep
    public void registerStandardExceptionMappers(BuildProducer<ResteasyJaxrsProviderBuildItem> providersProducer) {
        providersProducer
                .produce(new ResteasyJaxrsProviderBuildItem(ResteasyClassicResponseStatusExceptionMapper.class.getName()));
    }

    @BuildStep
    public void process(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ServletInitParamBuildItem> initParamProducer,
            BuildProducer<ResteasyDeploymentCustomizerBuildItem> deploymentCustomizerProducer) {

        validateControllers(beanArchiveIndexBuildItem);

        final IndexView index = beanArchiveIndexBuildItem.getIndex();
        final Collection<AnnotationInstance> annotations = index.getAnnotations(REST_CONTROLLER_ANNOTATION);
        if (annotations.isEmpty()) {
            return;
        }

        final Set<String> classNames = new HashSet<>();
        for (AnnotationInstance annotation : annotations) {
            classNames.add(annotation.target().asClass().toString());
        }

        // initialize the init params that will be used in case of servlet
        initParamProducer.produce(
                new ServletInitParamBuildItem(
                        ResteasyContextParameters.RESTEASY_SCANNED_RESOURCE_CLASSES_WITH_BUILDER,
                        SpringResourceBuilder.class.getName() + ":" + String.join(",", classNames)));
        // customize the deployment that will be used in case of RESTEasy standalone
        deploymentCustomizerProducer.produce(new ResteasyDeploymentCustomizerBuildItem(new Consumer<ResteasyDeployment>() {
            @Override
            public void accept(ResteasyDeployment resteasyDeployment) {
                resteasyDeployment.getScannedResourceClassesWithBuilder().put(SpringResourceBuilder.class.getName(),
                        new ArrayList<>(classNames));
            }
        }));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, SpringResourceBuilder.class.getName()));
    }

    /**
     * Make sure the controllers have the proper annotation and warn if not
     */
    private void validateControllers(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem) {
        Set<DotName> classesWithoutRestController = new HashSet<>();
        for (DotName mappingAnnotation : MAPPING_ANNOTATIONS) {
            Collection<AnnotationInstance> annotations = beanArchiveIndexBuildItem.getIndex().getAnnotations(mappingAnnotation);
            for (AnnotationInstance annotation : annotations) {
                ClassInfo targetClass;
                if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                    targetClass = annotation.target().asClass();
                } else if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                    targetClass = annotation.target().asMethod().declaringClass();
                } else {
                    continue;
                }

                if (targetClass.classAnnotation(REST_CONTROLLER_ANNOTATION) == null) {
                    classesWithoutRestController.add(targetClass.name());
                }
            }
        }

        if (!classesWithoutRestController.isEmpty()) {
            for (DotName dotName : classesWithoutRestController) {
                LOGGER.warn("Class '" + dotName
                        + "' uses a mapping annotation but the class itself was not annotated with '@RestContoller'. The mappings will therefore be ignored.");
            }
        }
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

        OUTER: for (DotName mappingClass : MAPPING_ANNOTATIONS) {
            final Collection<AnnotationInstance> instances = beanArchiveIndexBuildItem.getIndex().getAnnotations(mappingClass);
            for (AnnotationInstance instance : instances) {
                if (collectProviders(providersToRegister, categorizedWriters, instance, "produces")) {
                    useAllAvailable = true;
                    break OUTER;
                }
                if (collectProviders(providersToRegister, categorizedContextResolvers, instance, "produces")) {
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
        } else {
            // for Spring Web we register all the json providers by default because using "produces" in @RequestMapping
            // and friends is optional
            providersToRegister.addAll(categorizedWriters.getPossible(MediaType.APPLICATION_JSON_TYPE));
            // we also need to register the custom Spring related providers
            providersToRegister.add(ResponseEntityFeature.class.getName());
            providersToRegister.add(ResponseStatusFeature.class.getName());
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

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(STATIC_INIT)
    public void registerWithDevModeNotFoundMapper(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            ExceptionMapperRecorder recorder) {
        IndexView index = beanArchiveIndexBuildItem.getIndex();
        Collection<AnnotationInstance> restControllerAnnotations = index.getAnnotations(REST_CONTROLLER_ANNOTATION);
        if (restControllerAnnotations.isEmpty()) {
            return;
        }

        Map<String, NonJaxRsClassMappings> nonJaxRsPaths = new HashMap<>();
        for (AnnotationInstance restControllerInstance : restControllerAnnotations) {
            String basePath = "/";
            ClassInfo restControllerAnnotatedClass = restControllerInstance.target().asClass();

            AnnotationInstance requestMappingInstance = restControllerAnnotatedClass.classAnnotation(REQUEST_MAPPING);
            if (requestMappingInstance != null) {
                String basePathFromAnnotation = getMappingValue(requestMappingInstance);
                if (basePathFromAnnotation != null) {
                    basePath = basePathFromAnnotation;
                }
            }
            Map<String, String> methodNameToPath = new HashMap<>();
            NonJaxRsClassMappings nonJaxRsClassMappings = new NonJaxRsClassMappings();
            nonJaxRsClassMappings.setMethodNameToPath(methodNameToPath);
            nonJaxRsClassMappings.setBasePath(basePath);

            List<MethodInfo> methods = restControllerAnnotatedClass.methods();

            // go through each of the methods and see if there are any mapping Spring annotation from which to get the path
            METHOD: for (MethodInfo method : methods) {
                String methodName = method.name();
                String methodPath;
                // go through each of the annotations that can be used to make a method handle an http request
                for (DotName mappingClass : MAPPING_ANNOTATIONS) {
                    AnnotationInstance mappingClassAnnotation = method.annotation(mappingClass);
                    if (mappingClassAnnotation != null) {
                        methodPath = getMappingValue(mappingClassAnnotation);
                        if (methodPath == null) {
                            methodPath = ""; // ensure that no nasty null values show up in the output
                        } else if (!methodPath.startsWith("/")) {
                            methodPath = "/" + methodPath;
                        }
                        // record the mapping of method to the http path
                        methodNameToPath.put(methodName, methodPath);
                        continue METHOD;
                    }
                }
            }

            // if there was at least one controller method, add the controller since it contains methods that handle http requests
            if (!methodNameToPath.isEmpty()) {
                nonJaxRsPaths.put(restControllerAnnotatedClass.name().toString(), nonJaxRsClassMappings);
            }
        }

        if (!nonJaxRsPaths.isEmpty()) {
            recorder.nonJaxRsClassNameToMethodPaths(nonJaxRsPaths);
        }
    }

    /**
     * Meant to be called with an instance of any of the MAPPING_CLASSES
     */
    private String getMappingValue(AnnotationInstance instance) {
        if (instance == null) {
            return null;
        }
        if (instance.value() != null) {
            return instance.value().asStringArray()[0];
        } else if (instance.value("path") != null) {
            return instance.value("path").asStringArray()[0];
        }
        return null;
    }
}
