package io.quarkus.spring.web.deployment;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Providers;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.core.MediaTypeMap;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.metadata.SpringResourceBuilder;
import org.jboss.resteasy.spring.web.ResponseEntityFeature;
import org.jboss.resteasy.spring.web.ResponseStatusFeature;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.resteasy.common.deployment.ResteasyCommonProcessor;
import io.quarkus.resteasy.common.deployment.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.server.common.spi.AdditionalJaxRsResourceDefiningAnnotationBuildItem;
import io.quarkus.resteasy.server.common.spi.AdditionalJaxRsResourceMethodAnnotationsBuildItem;
import io.quarkus.resteasy.server.common.spi.AdditionalJaxRsResourceMethodParamAnnotations;
import io.quarkus.undertow.deployment.BlacklistedServletContainerInitializerBuildItem;
import io.quarkus.undertow.deployment.ServletInitParamBuildItem;

public class SpringWebProcessor {

    private static final DotName EXCEPTION = DotName.createSimple("java.lang.Exception");
    private static final DotName RUNTIME_EXCEPTION = DotName.createSimple("java.lang.RuntimeException");

    private static final DotName OBJECT = DotName.createSimple("java.lang.Object");
    private static final DotName STRING = DotName.createSimple("java.lang.String");

    private static final DotName REST_CONTROLLER_ANNOTATION = DotName
            .createSimple("org.springframework.web.bind.annotation.RestController");

    private static final DotName REQUEST_MAPPING = DotName
            .createSimple("org.springframework.web.bind.annotation.RequestMapping");
    private static final DotName PATH_VARIABLE = DotName.createSimple("org.springframework.web.bind.annotation.PathVariable");

    private static final List<DotName> MAPPING_CLASSES;

    static {
        MAPPING_CLASSES = Arrays.asList(
                REQUEST_MAPPING,
                DotName.createSimple("org.springframework.web.bind.annotation.GetMapping"),
                DotName.createSimple("org.springframework.web.bind.annotation.PostMapping"),
                DotName.createSimple("org.springframework.web.bind.annotation.PutMapping"),
                DotName.createSimple("org.springframework.web.bind.annotation.DeleteMapping"),
                DotName.createSimple("org.springframework.web.bind.annotation.PatchMapping"));
    }

    private static final DotName RESPONSE_STATUS = DotName
            .createSimple("org.springframework.web.bind.annotation.ResponseStatus");
    private static final DotName EXCEPTION_HANDLER = DotName
            .createSimple("org.springframework.web.bind.annotation.ExceptionHandler");

    private static final DotName REST_CONTROLLER_ADVICE = DotName
            .createSimple("org.springframework.web.bind.annotation.RestControllerAdvice");

    private static final DotName MODEL_AND_VIEW = DotName.createSimple("org.springframework.web.servlet.ModelAndView");
    private static final DotName VIEW = DotName.createSimple("org.springframework.web.servlet.View");
    private static final DotName MODEL = DotName.createSimple("org.springframework.ui.Model");

    private static final DotName HTTP_ENTITY = DotName.createSimple("org.springframework.http.HttpEntity");
    private static final DotName RESPONSE_ENTITY = DotName.createSimple("org.springframework.http.ResponseEntity");

    private static final Set<DotName> DISALLOWED_EXCEPTION_CONTROLLER_RETURN_TYPES = new HashSet<>(Arrays.asList(
            MODEL_AND_VIEW, VIEW, MODEL, HTTP_ENTITY, STRING));

    @BuildStep
    FeatureBuildItem registerFeature() {
        return new FeatureBuildItem(FeatureBuildItem.SPRING_WEB);
    }

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
                        PATH_VARIABLE,
                        DotName.createSimple("org.springframework.web.bind.annotation.RequestBody"),
                        DotName.createSimple("org.springframework.web.bind.annotation.MatrixVariable"),
                        DotName.createSimple("org.springframework.web.bind.annotation.RequestHeader"),
                        DotName.createSimple("org.springframework.web.bind.annotation.CookieValue")));
    }

    @BuildStep
    public void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(REST_CONTROLLER_ANNOTATION, BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(REST_CONTROLLER_ADVICE, BuiltinScope.SINGLETON.getName()));
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

        validate(annotations);

        final Set<String> classNames = new HashSet<>();
        for (AnnotationInstance annotation : annotations) {
            classNames.add(annotation.target().asClass().toString());
        }

        initParamProducer.produce(
                new ServletInitParamBuildItem(
                        ResteasyContextParameters.RESTEASY_SCANNED_RESOURCE_CLASSES_WITH_BUILDER,
                        SpringResourceBuilder.class.getName() + ":" + String.join(",", classNames)));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, SpringResourceBuilder.class.getName()));
    }

    private void validate(Collection<AnnotationInstance> restControllerInstances) {
        for (AnnotationInstance restControllerInstance : restControllerInstances) {
            ClassInfo restControllerClass = restControllerInstance.target().asClass();
            if (restControllerClass.classAnnotation(REQUEST_MAPPING) == null) {
                throw new IllegalArgumentException(
                        "Currently any class annotated with @RestController also needs to be annotated with @RequestMapping. " +
                                "Offending class is " + restControllerClass.name());
            }

            Map<DotName, List<AnnotationInstance>> annotations = restControllerClass.annotations();
            for (Map.Entry<DotName, List<AnnotationInstance>> entry : annotations.entrySet()) {
                DotName dotName = entry.getKey();
                if (PATH_VARIABLE.equals(dotName)) {
                    List<AnnotationInstance> pathVariableInstances = entry.getValue();
                    for (AnnotationInstance pathVariableInstance : pathVariableInstances) {
                        if (pathVariableInstance.target().kind() != AnnotationTarget.Kind.METHOD_PARAMETER) {
                            continue;
                        }
                        if ((pathVariableInstance.value() == null) && (pathVariableInstance.value("name") == null)) {
                            MethodInfo method = pathVariableInstance.target().asMethodParameter().method();
                            throw new IllegalArgumentException(
                                    "Currently method parameters annotated with @PathVariable must supply a value for 'name' or 'value'."
                                            +
                                            "Offending method is " + method.declaringClass().name() + "#" + method.name());
                        }

                    }
                }
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

    @BuildStep
    public void generateExceptionMapperProviders(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedExceptionMappers,
            BuildProducer<ResteasyJaxrsProviderBuildItem> providersProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer) {

        // Look for all exception classes that are annotated with @ResponseStatus

        IndexView index = beanArchiveIndexBuildItem.getIndex();
        ClassOutput classOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedExceptionMappers.produce(new GeneratedClassBuildItem(true, name, data));
            }
        };
        generateMappersForResponseStatusOnException(providersProducer, index, classOutput);
        generateMappersForExceptionHandlerInControllerAdvice(providersProducer, reflectiveClassProducer, index, classOutput);
    }

    private void generateMappersForResponseStatusOnException(BuildProducer<ResteasyJaxrsProviderBuildItem> providersProducer,
            IndexView index, ClassOutput classOutput) {
        Collection<AnnotationInstance> responseStatusInstances = index
                .getAnnotations(RESPONSE_STATUS);

        if (responseStatusInstances.isEmpty()) {
            return;
        }

        for (AnnotationInstance instance : responseStatusInstances) {
            if (AnnotationTarget.Kind.CLASS != instance.target().kind()) {
                continue;
            }
            if (!isException(instance.target().asClass(), index)) {
                continue;
            }

            String name = new ResponseStatusOnExceptionGenerator(instance.target().asClass(), classOutput).generate();
            providersProducer.produce(new ResteasyJaxrsProviderBuildItem(name));
        }
    }

    private void generateMappersForExceptionHandlerInControllerAdvice(
            BuildProducer<ResteasyJaxrsProviderBuildItem> providersProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer, IndexView index, ClassOutput classOutput) {

        AnnotationInstance controllerAdviceInstance = getSingleControllerAdviceInstance(index);
        if (controllerAdviceInstance == null) {
            return;
        }

        ClassInfo controllerAdvice = controllerAdviceInstance.target().asClass();
        List<MethodInfo> methods = controllerAdvice.methods();
        for (MethodInfo method : methods) {
            AnnotationInstance exceptionHandlerInstance = method.annotation(EXCEPTION_HANDLER);
            if (exceptionHandlerInstance == null) {
                continue;
            }

            if (!Modifier.isPublic(method.flags()) || Modifier.isStatic(method.flags())) {
                throw new IllegalStateException(
                        "@ExceptionHandler methods in @ControllerAdvice must be public instance methods");
            }

            DotName returnTypeDotName = method.returnType().name();
            if (DISALLOWED_EXCEPTION_CONTROLLER_RETURN_TYPES.contains(returnTypeDotName)) {
                throw new IllegalStateException(
                        "@ExceptionHandler methods in @ControllerAdvice classes can only have void, ResponseEntity or POJO return types");
            }

            if (!RESPONSE_ENTITY.equals(returnTypeDotName)) {
                reflectiveClassProducer.produce(new ReflectiveClassBuildItem(true, true, returnTypeDotName.toString()));
            }

            AnnotationInstance responseStatusInstance = method.annotation(RESPONSE_STATUS);
            if ((method.returnType().kind() == Type.Kind.VOID) && (responseStatusInstance == null)) {
                throw new IllegalStateException(
                        "void methods annotated with @ExceptionHandler must also be annotated with @ResponseStatus");
            }

            List<Type> parameters = method.parameters();
            boolean parametersSupported = true;
            if (parameters.size() > 1) {
                parametersSupported = false;
            } else if (parameters.size() == 1) {
                parametersSupported = isException(index.getClassByName(parameters.get(0).name()), index);
            }

            if (!parametersSupported) {
                throw new IllegalStateException(
                        "The only supported (optional) parameter type method for methods annotated with @ExceptionHandler is the exception type");
            }

            Type[] handledExceptionTypes = exceptionHandlerInstance.value().asClassArray();
            for (Type handledExceptionType : handledExceptionTypes) {
                String name = new ControllerAdviceAbstractExceptionMapperGenerator(method, handledExceptionType.name(),
                        classOutput, index).generate();
                providersProducer.produce(new ResteasyJaxrsProviderBuildItem(name));
            }

        }
    }

    private boolean isException(ClassInfo classInfo, IndexView index) {
        if (classInfo == null) {
            return false;
        }

        if (OBJECT.equals(classInfo.name())) {
            return false;
        }

        if (EXCEPTION.equals(classInfo.superName()) || RUNTIME_EXCEPTION.equals(classInfo.superName())) {
            return true;
        }

        return isException(index.getClassByName(classInfo.superName()), index);
    }

    private AnnotationInstance getSingleControllerAdviceInstance(IndexView index) {
        Collection<AnnotationInstance> controllerAdviceInstances = index.getAnnotations(REST_CONTROLLER_ADVICE);

        if (controllerAdviceInstances.isEmpty()) {
            return null;
        }

        if (controllerAdviceInstances.size() > 1) {
            throw new IllegalStateException("You can only have a single class annotated with @ControllerAdvice");
        }

        return controllerAdviceInstances.iterator().next();
    }

}
