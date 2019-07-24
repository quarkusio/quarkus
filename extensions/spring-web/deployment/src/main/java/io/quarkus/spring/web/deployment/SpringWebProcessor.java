package io.quarkus.spring.web.deployment;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
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
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
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

    private static final DotName EXCEPTION = DotName.createSimple("java.lang.Exception");
    private static final DotName RUNTIME_EXCEPTION = DotName.createSimple("java.lang.RuntimeException");
    private static final DotName OBJECT = DotName.createSimple("java.lang.Object");

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
        } else {
            // for Spring Web we register all the json providers by default because using "produces" in @RequestMapping
            // and friends is optional
            providersToRegister.addAll(categorizedWriters.getPossible(MediaType.APPLICATION_JSON_TYPE));
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
            BuildProducer<ResteasyJaxrsProviderBuildItem> providersProducer) {

        // Look for all exception classes that are annotated with @ResponseStatus

        IndexView index = beanArchiveIndexBuildItem.getIndex();
        Collection<AnnotationInstance> responseStatusInstances = index
                .getAnnotations(DotName.createSimple("org.springframework.web.bind.annotation.ResponseStatus"));

        if (responseStatusInstances.isEmpty()) {
            return;
        }

        ClassOutput classOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedExceptionMappers.produce(new GeneratedClassBuildItem(true, name, data));
            }
        };

        for (AnnotationInstance instance : responseStatusInstances) {
            if (AnnotationTarget.Kind.CLASS != instance.target().kind()) {
                continue;
            }
            if (!isException(instance.target().asClass(), index)) {
                continue;
            }

            String name = generateExceptionMapper(instance, classOutput);
            providersProducer.produce(new ResteasyJaxrsProviderBuildItem(name));
        }
    }

    private boolean isException(ClassInfo classInfo, IndexView index) {
        if (OBJECT.equals(classInfo.name())) {
            return false;
        }

        if (EXCEPTION.equals(classInfo.superName()) || RUNTIME_EXCEPTION.equals(classInfo.superName())) {
            return true;
        }

        return isException(index.getClassByName(classInfo.superName()), index);
    }

    /**
     * Generates an ExceptionMapper
     * Also generates a dummy subtype to get past the RESTEasy's check for synthetic classes
     */
    private String generateExceptionMapper(AnnotationInstance responseStatusInstance, ClassOutput classOutput) {
        ClassInfo exceptionClassInfo = responseStatusInstance.target().asClass();
        String generatedClassName = "io.quarkus.spring.web.mappers." + exceptionClassInfo.simpleName() + "Mapper";
        String generatedSubtypeClassName = "io.quarkus.spring.web.mappers.Subtype" + exceptionClassInfo.simpleName() + "Mapper";
        String exceptionClassName = exceptionClassInfo.name().toString();

        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(classOutput).className(generatedClassName)
                .interfaces(ExceptionMapper.class)
                .signature(String.format("Ljava/lang/Object;Ljavax/ws/rs/ext/ExceptionMapper<L%s;>;",
                        exceptionClassName.replace('.', '/')))
                .build()) {

            try (MethodCreator toResponse = cc.getMethodCreator("toResponse", Response.class.getName(), exceptionClassName)) {

                ResultHandle status = toResponse.load(getHttpStatusFromAnnotation(responseStatusInstance));
                ResultHandle responseBuilder = toResponse.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Response.class, "status", Response.ResponseBuilder.class, int.class),
                        status);
                ResultHandle exceptionMessage = toResponse.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(Throwable.class, "getMessage", String.class),
                        toResponse.getMethodParam(0));
                toResponse.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(Response.ResponseBuilder.class, "entity", Response.ResponseBuilder.class,
                                Object.class),
                        responseBuilder, exceptionMessage);
                ResultHandle httpResponseType = toResponse.load("text/plain");
                toResponse.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(Response.ResponseBuilder.class, "type", Response.ResponseBuilder.class,
                                String.class),
                        responseBuilder, httpResponseType);

                ResultHandle response = toResponse.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(Response.ResponseBuilder.class, "build", Response.class),
                        responseBuilder);
                toResponse.returnValue(response);
            }

            // bridge method
            try (MethodCreator bridgeToResponse = cc.getMethodCreator("toResponse", Response.class, Throwable.class)) {
                MethodDescriptor toResponse = MethodDescriptor.ofMethod(generatedClassName, "toResponse",
                        Response.class.getName(), exceptionClassName);
                ResultHandle castedObject = bridgeToResponse.checkCast(bridgeToResponse.getMethodParam(0), exceptionClassName);
                ResultHandle result = bridgeToResponse.invokeVirtualMethod(toResponse, bridgeToResponse.getThis(),
                        castedObject);
                bridgeToResponse.returnValue(result);
            }
        }

        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(classOutput).className(generatedSubtypeClassName)
                .superClass(generatedClassName)
                .build()) {
            cc.addAnnotation(Provider.class);
        }

        return generatedSubtypeClassName;
    }

    private int getHttpStatusFromAnnotation(AnnotationInstance responseStatusInstance) {
        AnnotationValue code = responseStatusInstance.value("code");
        if (code != null) {
            return enumValueToHttpStatus(code.asString());
        }

        AnnotationValue value = responseStatusInstance.value();
        if (value != null) {
            return enumValueToHttpStatus(value.asString());
        }

        return 500; // the default value of @ResponseStatus
    }

    private int enumValueToHttpStatus(String enumValue) {
        try {
            Class<?> httpStatusClass = Class.forName("org.springframework.http.HttpStatus");
            Enum correspondingEnum = Enum.valueOf((Class<Enum>) httpStatusClass, enumValue);
            Method valueMethod = httpStatusClass.getDeclaredMethod("value");
            return (int) valueMethod.invoke(correspondingEnum);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No spring web dependency found on the build classpath");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
