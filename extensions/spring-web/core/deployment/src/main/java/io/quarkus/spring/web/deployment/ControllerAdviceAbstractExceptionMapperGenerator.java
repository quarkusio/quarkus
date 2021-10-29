package io.quarkus.spring.web.deployment;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.spring.web.runtime.ResponseEntityConverter;

class ControllerAdviceAbstractExceptionMapperGenerator extends AbstractExceptionMapperGenerator {

    private static final DotName RESPONSE_ENTITY = DotName.createSimple("org.springframework.http.ResponseEntity");

    // Preferred content types order for String or primitive type responses
    private static final List<String> TEXT_MEDIA_TYPES = Arrays.asList(
            MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML);
    // Preferred content types order for object type responses
    private static final List<String> OBJECT_MEDIA_TYPES = Arrays.asList(
            MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_PLAIN);

    private final MethodInfo controllerAdviceMethod;
    private final TypesUtil typesUtil;
    private final Type returnType;
    private final List<Type> parameterTypes;
    private final String declaringClassName;

    private final Map<Type, FieldDescriptor> parameterTypeToField = new HashMap<>();

    private FieldDescriptor httpHeadersField;

    private final boolean isResteasyClassic;

    ControllerAdviceAbstractExceptionMapperGenerator(MethodInfo controllerAdviceMethod, DotName exceptionDotName,
            ClassOutput classOutput, TypesUtil typesUtil, boolean isResteasyClassic) {
        super(exceptionDotName, classOutput);

        // TODO: remove this restriction
        if (!isResteasyClassic) {
            throw new IllegalStateException("Currently Spring Web can only work with RESTEasy Classic");
        }

        this.controllerAdviceMethod = controllerAdviceMethod;
        this.typesUtil = typesUtil;

        this.returnType = controllerAdviceMethod.returnType();
        this.parameterTypes = controllerAdviceMethod.parameters();
        this.declaringClassName = controllerAdviceMethod.declaringClass().name().toString();
        this.isResteasyClassic = isResteasyClassic;
    }

    /**
     * We need to go through each parameter of the method of the ControllerAdvice
     * and make sure it's supported
     * The javax.ws.rs.ext.ExceptionMapper only has one parameter, the exception, however
     * other parameters can be obtained using @Context and therefore injected into the target method
     */
    @Override
    protected void preGenerateMethodBody(ClassCreator cc) {
        int notAllowedParameterIndex = -1;
        for (int i = 0; i < parameterTypes.size(); i++) {
            Type parameterType = parameterTypes.get(i);
            DotName parameterTypeDotName = parameterType.name();
            if (typesUtil.isAssignable(Exception.class, parameterTypeDotName)) {
                // do nothing since this will be handled during in generateMethodBody
            } else if (typesUtil.isAssignable(HttpServletRequest.class, parameterTypeDotName)) {
                if (parameterTypeToField.containsKey(parameterType)) {
                    throw new IllegalArgumentException("Parameter type " + parameterTypes.get(notAllowedParameterIndex).name()
                            + " is being used multiple times in method" + controllerAdviceMethod.name() + " of class"
                            + controllerAdviceMethod.declaringClass().name());
                }

                // we need to generate a field that injects the HttpServletRequest into the class
                FieldCreator httpRequestFieldCreator = cc.getFieldCreator("httpServletRequest", HttpServletRequest.class)
                        .setModifiers(Modifier.PRIVATE);
                httpRequestFieldCreator.addAnnotation(Context.class);

                // stash the fieldCreator in a map indexed by the parameter type so we can retrieve it later
                parameterTypeToField.put(parameterType, httpRequestFieldCreator.getFieldDescriptor());
            } else if (typesUtil.isAssignable(HttpServletResponse.class, parameterTypeDotName)) {
                if (parameterTypeToField.containsKey(parameterType)) {
                    throw new IllegalArgumentException("Parameter type " + parameterTypes.get(notAllowedParameterIndex).name()
                            + " is being used multiple times in method" + controllerAdviceMethod.name() + " of class"
                            + controllerAdviceMethod.declaringClass().name());
                }

                // we need to generate a field that injects the HttpServletRequest into the class
                FieldCreator httpRequestFieldCreator = cc.getFieldCreator("httpServletResponse", HttpServletResponse.class)
                        .setModifiers(Modifier.PRIVATE);
                httpRequestFieldCreator.addAnnotation(Context.class);

                // stash the fieldCreator in a map indexed by the parameter type so we can retrieve it later
                parameterTypeToField.put(parameterType, httpRequestFieldCreator.getFieldDescriptor());
            } else {
                notAllowedParameterIndex = i;
            }
        }
        if (notAllowedParameterIndex >= 0) {
            throw new IllegalArgumentException(
                    "Parameter type " + parameterTypes.get(notAllowedParameterIndex).name() + " is not supported for method"
                            + controllerAdviceMethod.name() + " of class" + controllerAdviceMethod.declaringClass().name());
        }

        createHttpHeadersField(cc);
    }

    private void createHttpHeadersField(ClassCreator classCreator) {
        FieldCreator httpHeadersFieldCreator = classCreator
                .getFieldCreator("httpHeaders", HttpHeaders.class)
                .setModifiers(Modifier.PRIVATE);
        httpHeadersFieldCreator.addAnnotation(Context.class);
        httpHeadersField = httpHeadersFieldCreator.getFieldDescriptor();
    }

    @Override
    void generateMethodBody(MethodCreator toResponse) {
        if (isVoidType(returnType)) {
            generateVoidExceptionHandler(toResponse);
        } else if (isEntityType(returnType)) {
            generateResponseEntityExceptionHandler(toResponse);
        } else {
            generateGenericResponseExceptionHandler(toResponse);
        }
    }

    private void generateVoidExceptionHandler(MethodCreator methodCreator) {
        invokeExceptionHandlerMethod(methodCreator);
        int status = getAnnotationStatusOrDefault(Response.Status.NO_CONTENT.getStatusCode());
        ResultHandle result = new ResponseBuilder(methodCreator, status)
                .withType(getResponseContentType(methodCreator, TEXT_MEDIA_TYPES))
                .build();
        methodCreator.returnValue(result);
    }

    private void generateResponseEntityExceptionHandler(MethodCreator methodCreator) {
        ResultHandle result = methodCreator.invokeStaticMethod(
                MethodDescriptor.ofMethod(ResponseEntityConverter.class.getName(), "toResponse",
                        Response.class.getName(), RESPONSE_ENTITY.toString(), MediaType.class.getName()),
                invokeExceptionHandlerMethod(methodCreator),
                getResponseContentType(methodCreator, getSupportedMediaTypesForType(getResponseEntityType())));

        methodCreator.returnValue(result);
    }

    private Type getResponseEntityType() {
        if (isParameterizedType(returnType) && returnType.asParameterizedType().arguments().size() == 1) {
            return returnType.asParameterizedType().arguments().get(0);
        }
        return returnType;
    }

    private void generateGenericResponseExceptionHandler(MethodCreator methodCreator) {
        int status = getAnnotationStatusOrDefault(Response.Status.OK.getStatusCode());
        ResultHandle result = new ResponseBuilder(methodCreator, status)
                .withEntity(invokeExceptionHandlerMethod(methodCreator))
                .withType(getResponseContentType(methodCreator, getSupportedMediaTypesForType(returnType)))
                .build();

        methodCreator.returnValue(result);
    }

    private List<String> getSupportedMediaTypesForType(Type type) {
        if (isStringType(type) || isPrimitiveType(type)) {
            return TEXT_MEDIA_TYPES;
        }

        return OBJECT_MEDIA_TYPES;
    }

    private ResultHandle getResponseContentType(MethodCreator methodCreator, List<String> supportedMediaTypeStrings) {
        ResultHandle[] supportedMediaTypes = supportedMediaTypeStrings.stream()
                .map(methodCreator::load)
                .toArray(ResultHandle[]::new);

        String responseContentTypeResolverClassName = isResteasyClassic
                ? "io.quarkus.spring.web.runtime.ResteasyClassicResponseContentTypeResolver"
                : "io.quarkus.spring.web.runtime.ResteasyReactiveResponseContentTypeResolver"; // doesn't exist yet
        ResultHandle contentTypeResolver = methodCreator
                .newInstance(MethodDescriptor.ofConstructor(responseContentTypeResolverClassName));
        return methodCreator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(responseContentTypeResolverClassName, "resolve", MediaType.class,
                        HttpHeaders.class, String[].class),
                contentTypeResolver,
                methodCreator.readInstanceField(httpHeadersField, methodCreator.getThis()),
                methodCreator.marshalAsArray(String.class, supportedMediaTypes));
    }

    private ResultHandle invokeExceptionHandlerMethod(MethodCreator toResponse) {
        String returnTypeClassName = isVoidType(returnType) ? void.class.getName() : returnType.name().toString();

        if (parameterTypes.isEmpty()) {
            return toResponse.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(declaringClassName, controllerAdviceMethod.name(), returnTypeClassName),
                    controllerAdviceInstance(toResponse));
        }

        String[] parameterTypesStr = new String[parameterTypes.size()];
        ResultHandle[] parameterTypeHandles = new ResultHandle[parameterTypes.size()];
        for (int i = 0; i < parameterTypes.size(); i++) {
            Type parameterType = parameterTypes.get(i);
            parameterTypesStr[i] = parameterType.name().toString();
            if (typesUtil.isAssignable(Exception.class, parameterType.name())) {
                parameterTypeHandles[i] = toResponse.getMethodParam(i);
            } else {
                parameterTypeHandles[i] = toResponse.readInstanceField(parameterTypeToField.get(parameterType),
                        toResponse.getThis());
            }
        }

        return toResponse.invokeVirtualMethod(
                MethodDescriptor.ofMethod(declaringClassName, controllerAdviceMethod.name(), returnTypeClassName,
                        parameterTypesStr),
                controllerAdviceInstance(toResponse), parameterTypeHandles);
    }

    private ResultHandle controllerAdviceInstance(MethodCreator toResponse) {
        ResultHandle controllerAdviceClass = toResponse.loadClass(declaringClassName);

        ResultHandle container = toResponse
                .invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle instance = toResponse.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class,
                        Annotation[].class),
                container, controllerAdviceClass, toResponse.loadNull());
        ResultHandle bean = toResponse.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class),
                instance);
        return toResponse.checkCast(bean, controllerAdviceMethod.declaringClass().name().toString());
    }

    private int getAnnotationStatusOrDefault(int defaultValue) {
        AnnotationInstance annotation = controllerAdviceMethod.annotation(RESPONSE_STATUS);
        if (annotation == null) {
            return defaultValue;
        }

        return getHttpStatusFromAnnotation(annotation);
    }

    private boolean isVoidType(Type type) {
        return Type.Kind.VOID.equals(type.kind());
    }

    private boolean isPrimitiveType(Type type) {
        return Type.Kind.PRIMITIVE.equals(type.kind());
    }

    private boolean isStringType(Type type) {
        return DotName.createSimple(String.class.getName()).equals(type.name());
    }

    private boolean isEntityType(Type type) {
        return RESPONSE_ENTITY.equals(type.name());
    }

    private boolean isParameterizedType(Type type) {
        return Type.Kind.PARAMETERIZED_TYPE.equals(type.kind());
    }
}
