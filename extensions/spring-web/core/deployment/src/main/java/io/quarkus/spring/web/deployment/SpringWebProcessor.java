package io.quarkus.spring.web.deployment;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Priorities;
import javax.ws.rs.core.HttpHeaders;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.jaxrs.spi.deployment.AdditionalJaxRsResourceMethodAnnotationsBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.reactive.spi.ExceptionMapperBuildItem;

public class SpringWebProcessor {

    private static final DotName REST_CONTROLLER_ANNOTATION = DotName
            .createSimple("org.springframework.web.bind.annotation.RestController");

    private static final DotName REQUEST_MAPPING = DotName
            .createSimple("org.springframework.web.bind.annotation.RequestMapping");
    private static final DotName PATH_VARIABLE = DotName.createSimple("org.springframework.web.bind.annotation.PathVariable");

    private static final List<DotName> MAPPING_ANNOTATIONS;

    static {
        MAPPING_ANNOTATIONS = Arrays.asList(
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
            MODEL_AND_VIEW, VIEW, MODEL, HTTP_ENTITY));

    @BuildStep
    FeatureBuildItem registerFeature() {
        return new FeatureBuildItem(Feature.SPRING_WEB);
    }

    @BuildStep
    public AdditionalJaxRsResourceMethodAnnotationsBuildItem additionalJaxRsResourceMethodAnnotationsBuildItem() {
        return new AdditionalJaxRsResourceMethodAnnotationsBuildItem(MAPPING_ANNOTATIONS);
    }

    @BuildStep
    public void ignoreReflectionHierarchy(BuildProducer<ReflectiveHierarchyIgnoreWarningBuildItem> ignore) {
        ignore.produce(new ReflectiveHierarchyIgnoreWarningBuildItem(
                new ReflectiveHierarchyIgnoreWarningBuildItem.DotNameExclusion(RESPONSE_ENTITY)));
        ignore.produce(
                new ReflectiveHierarchyIgnoreWarningBuildItem(new ReflectiveHierarchyIgnoreWarningBuildItem.DotNameExclusion(
                        DotName.createSimple("org.springframework.util.MimeType"))));
        ignore.produce(
                new ReflectiveHierarchyIgnoreWarningBuildItem(new ReflectiveHierarchyIgnoreWarningBuildItem.DotNameExclusion(
                        DotName.createSimple("org.springframework.util.MultiValueMap"))));
    }

    @BuildStep
    public void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(REST_CONTROLLER_ANNOTATION, BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(REST_CONTROLLER_ADVICE, BuiltinScope.SINGLETON.getName()));
    }

    @BuildStep
    public void exceptionHandlingSupport(CombinedIndexBuildItem index,
            BuildProducer<GeneratedClassBuildItem> generatedExceptionMappers,
            BuildProducer<ResteasyJaxrsProviderBuildItem> providersProducer,
            BuildProducer<ExceptionMapperBuildItem> exceptionMapperProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeanProducer,
            Capabilities capabilities) {

        boolean isResteasyClassicAvailable = capabilities.isPresent(Capability.RESTEASY_JSON_JACKSON);
        boolean isResteasyReactiveAvailable = capabilities.isPresent(Capability.RESTEASY_REACTIVE_JSON_JACKSON);

        if (!isResteasyClassicAvailable && !isResteasyReactiveAvailable) {
            throw new IllegalStateException(
                    "Spring Web can only work if 'quarkus-resteasy-jackson' or 'quarkus-resteasy-reactive-jackson' is present");
        }

        TypesUtil typesUtil = new TypesUtil(Thread.currentThread().getContextClassLoader());

        // Look for all exception classes that are annotated with @ResponseStatus

        IndexView indexView = index.getIndex();
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedExceptionMappers, true);
        generateMappersForResponseStatusOnException(providersProducer, exceptionMapperProducer, indexView, classOutput,
                typesUtil,
                isResteasyClassicAvailable);
        generateMappersForExceptionHandlerInControllerAdvice(providersProducer, exceptionMapperProducer,
                reflectiveClassProducer, unremovableBeanProducer, indexView, classOutput,
                typesUtil, isResteasyClassicAvailable);
    }

    private void generateMappersForResponseStatusOnException(BuildProducer<ResteasyJaxrsProviderBuildItem> providersProducer,
            BuildProducer<ExceptionMapperBuildItem> exceptionMapperProducer,
            IndexView index, ClassOutput classOutput, TypesUtil typesUtil, boolean isResteasyClassic) {
        Collection<AnnotationInstance> responseStatusInstances = index
                .getAnnotations(RESPONSE_STATUS);

        if (responseStatusInstances.isEmpty()) {
            return;
        }

        for (AnnotationInstance instance : responseStatusInstances) {
            if (AnnotationTarget.Kind.CLASS != instance.target().kind()) {
                continue;
            }
            DotName dotName = instance.target().asClass().name();
            if (!typesUtil.isAssignable(Exception.class, dotName)) {
                continue;
            }

            String name = new ResponseStatusOnExceptionGenerator(instance.target().asClass(), classOutput, isResteasyClassic)
                    .generate();
            providersProducer.produce(new ResteasyJaxrsProviderBuildItem(name));
            exceptionMapperProducer.produce(
                    new ExceptionMapperBuildItem(name, dotName.toString(), Priorities.USER, false));
        }
    }

    private void generateMappersForExceptionHandlerInControllerAdvice(
            BuildProducer<ResteasyJaxrsProviderBuildItem> providersProducer,
            BuildProducer<ExceptionMapperBuildItem> exceptionMapperProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeanProducer, IndexView index, ClassOutput classOutput,
            TypesUtil typesUtil, boolean isResteasyClassic) {

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

            // we need to generate one JAX-RS ExceptionMapper per Exception type
            Type[] handledExceptionTypes = exceptionHandlerInstance.value().asClassArray();
            for (Type handledExceptionType : handledExceptionTypes) {
                String name = new ControllerAdviceExceptionMapperGenerator(method, handledExceptionType.name(),
                        classOutput, typesUtil, isResteasyClassic).generate();
                providersProducer.produce(new ResteasyJaxrsProviderBuildItem(name));
                exceptionMapperProducer.produce(
                        new ExceptionMapperBuildItem(name, handledExceptionType.name().toString(), Priorities.USER, false));
            }

        }

        // allow access to HttpHeaders from Arc.container()
        if (!isResteasyClassic) {
            unremovableBeanProducer.produce(
                    UnremovableBeanBuildItem.beanClassNames("org.jboss.resteasy.reactive.server.injection.ContextProducers",
                            HttpHeaders.class.getName()));
        }
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
