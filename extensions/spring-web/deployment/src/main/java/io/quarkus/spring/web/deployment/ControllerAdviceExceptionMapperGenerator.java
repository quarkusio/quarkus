package io.quarkus.spring.web.deployment;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.spring.web.runtime.ResponseContentTypeResolver;
import io.quarkus.spring.web.runtime.ResponseEntityConverter;

class ControllerAdviceExceptionMapperGenerator extends AbstractExceptionMapperGenerator {

    private static final DotName RESPONSE_ENTITY = DotName.createSimple("org.springframework.http.ResponseEntity");

    // Preferred content types order for String or primitive type responses
    private static final List<String> TEXT_MEDIA_TYPES = Arrays.asList(
            MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON);
    // Preferred content types order for object type responses
    private static final List<String> OBJECT_MEDIA_TYPES = Arrays.asList(
            MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);

    private final MethodInfo controllerAdviceMethod;
    private final TypesUtil typesUtil;
    private final Type returnType;
    private final List<Type> parameterTypes;
    private final String declaringClassName;

    ControllerAdviceExceptionMapperGenerator(MethodInfo controllerAdviceMethod, DotName exceptionDotName,
            ClassOutput classOutput, TypesUtil typesUtil) {
        super(exceptionDotName, classOutput);
        this.controllerAdviceMethod = controllerAdviceMethod;
        this.typesUtil = typesUtil;

        this.returnType = controllerAdviceMethod.returnType();
        this.parameterTypes = controllerAdviceMethod.parameters();
        this.declaringClassName = controllerAdviceMethod.declaringClass().name().toString();
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

        return methodCreator.invokeStaticMethod(
                MethodDescriptor.ofMethod(ResponseContentTypeResolver.class, "resolve", MediaType.class,
                        HttpHeaders.class, String[].class),
                getBeanFromArc(methodCreator, HttpHeaders.class.getName()),
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
            } else if (typesUtil.isAssignable(UriInfo.class, parameterType.name())) {
                parameterTypeHandles[i] = getBeanFromArc(toResponse, UriInfo.class.getName());
            } else if (typesUtil.isAssignable(Request.class, parameterType.name())) {
                parameterTypeHandles[i] = getBeanFromArc(toResponse, Request.class.getName());
            } else {
                throw new IllegalArgumentException(
                        "Parameter type '" + parameterType.name() + "' is not supported for method '"
                                + controllerAdviceMethod.name() + "' of class '"
                                + controllerAdviceMethod.declaringClass().name()
                                + "'");
            }
        }

        return toResponse.invokeVirtualMethod(
                MethodDescriptor.ofMethod(declaringClassName, controllerAdviceMethod.name(), returnTypeClassName,
                        parameterTypesStr),
                controllerAdviceInstance(toResponse), parameterTypeHandles);
    }

    private ResultHandle controllerAdviceInstance(MethodCreator toResponse) {
        return toResponse.checkCast(getBeanFromArc(toResponse, declaringClassName),
                controllerAdviceMethod.declaringClass().name().toString());
    }

    private ResultHandle getBeanFromArc(MethodCreator methodCreator, String beanClassName) {
        ResultHandle container = methodCreator
                .invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle instance = methodCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class,
                        Annotation[].class),
                container, methodCreator.loadClass(beanClassName), methodCreator.loadNull());
        return methodCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class),
                instance);
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
