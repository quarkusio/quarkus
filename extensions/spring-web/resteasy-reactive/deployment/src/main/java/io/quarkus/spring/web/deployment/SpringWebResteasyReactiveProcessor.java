package io.quarkus.spring.web.deployment;

import static org.jboss.jandex.AnnotationInstance.create;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.DEFAULT_VALUE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_COOKIE_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_MATRIX_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_PATH_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_QUERY_PARAM;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.scanning.ResteasyReactiveScanner;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationsTransformer;
import org.jboss.resteasy.reactive.common.processor.transformation.Transformation;
import org.jboss.resteasy.reactive.server.model.FixedHandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.resteasy.reactive.server.spi.AnnotationsTransformerBuildItem;
import io.quarkus.resteasy.reactive.server.spi.MethodScannerBuildItem;
import io.quarkus.resteasy.reactive.spi.AdditionalResourceClassBuildItem;
import io.quarkus.resteasy.reactive.spi.ExceptionMapperBuildItem;
import io.quarkus.resteasy.server.common.spi.AdditionalJaxRsResourceMethodParamAnnotations;
import io.quarkus.spring.web.runtime.ResponseEntityHandler;
import io.quarkus.spring.web.runtime.ResponseStatusHandler;
import io.quarkus.spring.web.runtime.common.ResponseStatusExceptionMapper;

public class SpringWebResteasyReactiveProcessor {

    private static final Logger LOGGER = Logger.getLogger(SpringWebResteasyReactiveProcessor.class.getName());

    private static final DotName REST_CONTROLLER_ANNOTATION = DotName
            .createSimple("org.springframework.web.bind.annotation.RestController");

    private static final DotName REQUEST_MAPPING = DotName
            .createSimple("org.springframework.web.bind.annotation.RequestMapping");
    private static final DotName GET_MAPPING = DotName.createSimple("org.springframework.web.bind.annotation.GetMapping");
    private static final DotName POST_MAPPING = DotName.createSimple("org.springframework.web.bind.annotation.PostMapping");
    private static final DotName PUT_MAPPING = DotName.createSimple("org.springframework.web.bind.annotation.PutMapping");
    private static final DotName DELETE_MAPPING = DotName.createSimple("org.springframework.web.bind.annotation.DeleteMapping");
    private static final DotName PATCH_MAPPING = DotName.createSimple("org.springframework.web.bind.annotation.PatchMapping");
    private static final List<DotName> MAPPING_ANNOTATIONS = List.of(REQUEST_MAPPING, GET_MAPPING, POST_MAPPING,
            PUT_MAPPING, DELETE_MAPPING, PATCH_MAPPING);

    private static final DotName PATH_VARIABLE = DotName.createSimple("org.springframework.web.bind.annotation.PathVariable");
    private static final DotName REQUEST_PARAM = DotName.createSimple("org.springframework.web.bind.annotation.RequestParam");
    private static final DotName REQUEST_HEADER = DotName.createSimple("org.springframework.web.bind.annotation.RequestHeader");
    private static final DotName COOKIE_VALUE = DotName.createSimple("org.springframework.web.bind.annotation.CookieValue");
    private static final DotName MATRIX_VARIABLE = DotName
            .createSimple("org.springframework.web.bind.annotation.MatrixVariable");

    private static final DotName RESPONSE_STATUS = DotName
            .createSimple("org.springframework.web.bind.annotation.ResponseStatus");

    private static final DotName MODEL_AND_VIEW = DotName.createSimple("org.springframework.web.servlet.ModelAndView");
    private static final DotName VIEW = DotName.createSimple("org.springframework.web.servlet.View");
    private static final DotName MODEL = DotName.createSimple("org.springframework.ui.Model");

    private static final DotName HTTP_ENTITY = DotName.createSimple("org.springframework.http.HttpEntity");
    private static final DotName RESPONSE_ENTITY = DotName.createSimple("org.springframework.http.ResponseEntity");

    private static final String DEFAULT_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n"; // from ValueConstants

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
    public void registerStandardExceptionMappers(BuildProducer<ExceptionMapperBuildItem> producer) {
        producer.produce(new ExceptionMapperBuildItem(ResponseStatusExceptionMapper.class.getName(),
                ResponseStatusException.class.getName(), Priorities.USER, false));
    }

    @BuildStep
    public void registerAdditionalResourceClasses(CombinedIndexBuildItem index,
            BuildProducer<AdditionalResourceClassBuildItem> additionalResourceClassProducer) {

        validateControllers(index.getIndex());

        for (AnnotationInstance restController : index.getIndex()
                .getAnnotations(REST_CONTROLLER_ANNOTATION)) {
            ClassInfo targetClass = restController.target().asClass();
            additionalResourceClassProducer.produce(new AdditionalResourceClassBuildItem(targetClass,
                    getSinglePathOfInstance(targetClass.declaredAnnotation(REQUEST_MAPPING), "")));
        }
    }

    /**
     * Make sure the controllers have the proper annotation and warn if not
     */
    private void validateControllers(IndexView index) {
        Set<DotName> classesWithoutRestController = new HashSet<>();
        for (DotName mappingAnnotation : MAPPING_ANNOTATIONS) {
            Collection<AnnotationInstance> annotations = index.getAnnotations(mappingAnnotation);
            for (AnnotationInstance annotation : annotations) {
                ClassInfo targetClass;
                if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                    targetClass = annotation.target().asClass();
                } else if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                    targetClass = annotation.target().asMethod().declaringClass();
                } else {
                    continue;
                }

                if (targetClass.declaredAnnotation(REST_CONTROLLER_ANNOTATION) == null) {
                    classesWithoutRestController.add(targetClass.name());
                }
            }
        }

        if (!classesWithoutRestController.isEmpty()) {
            for (DotName dotName : classesWithoutRestController) {
                LOGGER.warn("Class '" + dotName
                        + "' uses a mapping annotation but the class itself was not annotated with '@RestController'. The mappings will therefore be ignored.");
            }
        }
    }

    // meant to be called with instances of MAPPING_ANNOTATIONS
    private static String getSinglePathOfInstance(AnnotationInstance instance, String defaultPathValue) {
        String[] paths = getPathsOfInstance(instance);
        if ((paths != null) && (paths.length > 0)) {
            return paths[0];
        }
        return defaultPathValue;
    }

    // meant to be called with instances of MAPPING_ANNOTATIONS
    private static String[] getPathsOfInstance(AnnotationInstance instance) {
        if (instance == null) {
            return null;
        }
        AnnotationValue pathValue = instance.value("path");
        if (pathValue != null) {
            return pathValue.asStringArray();
        }
        AnnotationValue value = instance.value();
        if (value != null) {
            return value.asStringArray();
        }
        return null;
    }

    // meant to be called with instances of MAPPING_ANNOTATIONS and a property name that contains a String array value
    private static String[] getStringArrayValueOfInstance(AnnotationInstance instance, String property) {
        if (instance == null) {
            return null;
        }
        AnnotationValue pathValue = instance.value(property);
        if (pathValue != null) {
            return pathValue.asStringArray();
        }
        return null;
    }

    @BuildStep
    public void methodAnnotationsTransformer(BuildProducer<AnnotationsTransformerBuildItem> producer) {

        producer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.METHOD;
            }

            @Override
            public void transform(TransformationContext transformationContext) {
                AnnotationTarget target = transformationContext.getTarget();
                if (target.kind() != AnnotationTarget.Kind.METHOD) {
                    return;
                }
                MethodInfo methodInfo = target.asMethod();
                Transformation transform = transformationContext.transform();
                DotName jaxRSMethodAnnotation = null;
                String path = null;
                String[] produces = null;
                String[] consumes = null;

                AnnotationInstance mappingAnnotationInstance = methodInfo.annotation(REQUEST_MAPPING);
                if (mappingAnnotationInstance != null) {
                    AnnotationValue methodValue = mappingAnnotationInstance.value("method");
                    if (methodValue == null) {
                        jaxRSMethodAnnotation = ResteasyReactiveDotNames.GET;
                    } else {
                        String[] methods = methodValue.asEnumArray();
                        if (methods.length > 1) {
                            throw new IllegalArgumentException(
                                    "Usage of multiple methods using '@RequestMapping' is not allowed. Offending method is '"
                                            + methodInfo.declaringClass().name() + "#" + methodInfo.name() + "'");
                        }
                        DotName methodDotName = ResteasyReactiveScanner.METHOD_TO_BUILTIN_HTTP_ANNOTATIONS.get(methods[0]);
                        if (methodDotName == null) {
                            throw new IllegalArgumentException(
                                    "Unsupported HTTP method '" + methods[0] + "' for @RequestMapping. Offending method is '"
                                            + methodInfo.declaringClass().name() + "#" + methodInfo.name() + "'");
                        }
                        jaxRSMethodAnnotation = methodDotName;
                    }
                } else {
                    if (methodInfo.hasAnnotation(GET_MAPPING)) {
                        jaxRSMethodAnnotation = ResteasyReactiveDotNames.GET;
                        mappingAnnotationInstance = methodInfo.annotation(GET_MAPPING);
                    } else if (methodInfo.hasAnnotation(POST_MAPPING)) {
                        jaxRSMethodAnnotation = ResteasyReactiveDotNames.POST;
                        mappingAnnotationInstance = methodInfo.annotation(POST_MAPPING);
                    } else if (methodInfo.hasAnnotation(PUT_MAPPING)) {
                        jaxRSMethodAnnotation = ResteasyReactiveDotNames.PUT;
                        mappingAnnotationInstance = methodInfo.annotation(PUT_MAPPING);
                    } else if (methodInfo.hasAnnotation(DELETE_MAPPING)) {
                        jaxRSMethodAnnotation = ResteasyReactiveDotNames.DELETE;
                        mappingAnnotationInstance = methodInfo.annotation(DELETE_MAPPING);
                    } else if (methodInfo.hasAnnotation(PATCH_MAPPING)) {
                        jaxRSMethodAnnotation = ResteasyReactiveDotNames.PATCH;
                        mappingAnnotationInstance = methodInfo.annotation(PATCH_MAPPING);
                    }
                }

                if (jaxRSMethodAnnotation == null) {
                    return;
                }

                produces = getStringArrayValueOfInstance(mappingAnnotationInstance, "produces");
                consumes = getStringArrayValueOfInstance(mappingAnnotationInstance, "consumes");
                path = getSinglePathOfInstance(mappingAnnotationInstance, null);

                transform.add(jaxRSMethodAnnotation);
                addStringArrayValuedAnnotation(transform, target, consumes, ResteasyReactiveDotNames.CONSUMES);
                addStringArrayValuedAnnotation(transform, target, produces, ResteasyReactiveDotNames.PRODUCES);
                addPathAnnotation(transform, target, path);

                for (AnnotationInstance annotation : methodInfo.annotations()) {
                    if (annotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                        DotName annotationName = annotation.name();
                        //TODO: add Cookie and Matrix handling
                        if (annotationName.equals(REQUEST_PARAM)
                                || annotationName.equals(REQUEST_HEADER)
                                || annotationName.equals(COOKIE_VALUE)
                                || annotationName.equals(MATRIX_VARIABLE)) {

                            DotName jaxRsAnnotation;
                            if (annotationName.equals(REQUEST_PARAM)) {
                                jaxRsAnnotation = REST_QUERY_PARAM;
                            } else if (annotationName.equals(REQUEST_HEADER)) {
                                jaxRsAnnotation = REST_QUERY_PARAM;
                            } else if (annotationName.equals(COOKIE_VALUE)) {
                                jaxRsAnnotation = REST_COOKIE_PARAM;
                            } else {
                                jaxRsAnnotation = REST_MATRIX_PARAM;
                            }

                            String name = getNameOrDefaultFromParamAnnotation(annotation);
                            List<AnnotationValue> annotationValues;
                            if (name == null) {
                                annotationValues = Collections.emptyList();

                            } else {
                                annotationValues = Collections.singletonList(AnnotationValue.createStringValue("value", name));
                            }
                            transform.add(create(jaxRsAnnotation, annotation.target(), annotationValues));

                            boolean required = true; // the default value
                            String defaultValueStr = DEFAULT_NONE; // default value of @RequestMapping#defaultValue
                            AnnotationValue defaultValue = annotation.value("defaultValue");
                            if (defaultValue != null) {
                                defaultValueStr = defaultValue.asString();
                                required = false; // implicitly set according to the javadoc of @RequestMapping#defaultValue
                            } else {
                                AnnotationValue requiredValue = annotation.value("required");
                                if (requiredValue != null) {
                                    required = requiredValue.asBoolean();
                                }
                            }
                            if (!required) {
                                transform.add(create(DEFAULT_VALUE, annotation.target(),
                                        Collections
                                                .singletonList(AnnotationValue.createStringValue("value", defaultValueStr))));
                            }
                        } else if (annotationName.equals(PATH_VARIABLE)) {
                            String name = getNameOrDefaultFromParamAnnotation(annotation);
                            List<AnnotationValue> annotationValues = Collections.emptyList();
                            if (name != null) {
                                annotationValues = Collections.singletonList(AnnotationValue.createStringValue("value", name));
                            }
                            transform.add(create(REST_PATH_PARAM, annotation.target(), annotationValues));
                        }
                    }
                }

                transform.done();
            }

            private String getNameOrDefaultFromParamAnnotation(AnnotationInstance annotation) {
                AnnotationValue nameValue = annotation.value("name");
                if (nameValue != null) {
                    return nameValue.asString();
                } else {
                    AnnotationValue value = annotation.value();
                    if (value != null) {
                        return value.asString();
                    }
                }
                return null;
            }

            private void addStringArrayValuedAnnotation(Transformation transform, AnnotationTarget target, String[] value,
                    DotName annotationDotName) {
                if ((value != null) && value.length > 0) {
                    AnnotationValue[] values = new AnnotationValue[value.length];
                    for (int i = 0; i < values.length; i++) {
                        values[i] = AnnotationValue.createStringValue("", value[i]);
                    }
                    transform.add(AnnotationInstance.create(annotationDotName, target,
                            new AnnotationValue[] { AnnotationValue.createArrayValue("value", values) }));
                }
            }

            private void addPathAnnotation(Transformation transform, AnnotationTarget target, String path) {
                if (path == null) {
                    return;
                }
                transform.add(AnnotationInstance.create(ResteasyReactiveDotNames.PATH, target,
                        new AnnotationValue[] { AnnotationValue.createStringValue("value", replaceSpringWebWildcards(path)) }));
            }

            private String replaceSpringWebWildcards(String methodPath) {
                if (methodPath.contains("/**")) {
                    methodPath = methodPath.replace("/**", "{unsetPlaceHolderVar:.*}");
                }
                if (methodPath.contains("/*")) {
                    methodPath = methodPath.replace("/*", "/{unusedPlaceHolderVar}");
                }
                /*
                 * Spring Web allows the use of '?' to capture a single character. We support this by
                 * converting each url path using it to a JAX-RS syntax of variable followed by a regex.
                 * So '/car?/s?o?/info' would become '/{notusedPlaceHolderVar0:car.}/{notusedPlaceHolderVar1:s.o.}/info'
                 */
                String[] parts = methodPath.split("/");
                if (parts.length > 0) {
                    StringBuilder sb = new StringBuilder(methodPath.startsWith("/") ? "/" : "");
                    for (int i = 0; i < parts.length; i++) {
                        String part = parts[i];
                        if (part.isEmpty()) {
                            continue;
                        }
                        if (!sb.toString().endsWith("/")) {
                            sb.append("/");
                        }
                        if ((part.startsWith("{") && part.endsWith("}")) || !part.contains("?")) {
                            sb.append(part);
                        } else {
                            sb.append(String.format("{notusedPlaceHolderVar%s:", i)).append(part.replace('?', '.')).append("}");
                        }
                    }
                    if (methodPath.endsWith("/")) {
                        sb.append("/");
                    }
                    methodPath = sb.toString();
                }
                return methodPath;
            }

        }));
    }

    @BuildStep
    public MethodScannerBuildItem responseEntitySupport() {
        return new MethodScannerBuildItem(new MethodScanner() {
            @Override
            public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
                    Map<String, Object> methodContext) {
                DotName returnTypeName = method.returnType().name();
                if (returnTypeName.equals(RESPONSE_ENTITY)) {
                    return Collections.singletonList(new FixedHandlerChainCustomizer(new ResponseEntityHandler(),
                            HandlerChainCustomizer.Phase.AFTER_METHOD_INVOKE));
                }
                return Collections.emptyList();
            }
        });
    }

    //TODO: replace with RESTEasy Reactive @ResponseStatus support using an annotation transformer
    @BuildStep
    public MethodScannerBuildItem responseStatusSupport() {
        return new MethodScannerBuildItem(new MethodScanner() {
            @Override
            public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
                    Map<String, Object> methodContext) {
                AnnotationInstance responseStatus = method.annotation(RESPONSE_STATUS);
                if (responseStatus != null) {
                    int newStatus = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(); // default value for @ResponseStatus
                    AnnotationValue codeValue = responseStatus.value("code");
                    if (codeValue != null) {
                        newStatus = HttpStatus.valueOf(codeValue.asEnum()).value();
                    } else {
                        AnnotationValue value = responseStatus.value();
                        if (value != null) {
                            newStatus = HttpStatus.valueOf(value.asEnum()).value();
                        }
                    }

                    ResponseStatusHandler handler = new ResponseStatusHandler();
                    handler.setNewResponseCode(newStatus);
                    handler.setDefaultResponseCode(
                            method.returnType().kind() != Type.Kind.VOID ? Response.Status.OK.getStatusCode()
                                    : Response.Status.NO_CONTENT.getStatusCode());
                    return Collections.singletonList(
                            new FixedHandlerChainCustomizer(handler, HandlerChainCustomizer.Phase.AFTER_RESPONSE_CREATED));
                }

                return Collections.emptyList();
            }
        });
    }
}
