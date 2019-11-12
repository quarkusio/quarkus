package io.quarkus.spring.web.deployment;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
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

    private final MethodInfo controllerAdviceMethod;
    private final TypesUtil typesUtil;
    private final Type returnType;
    private final List<Type> parameterTypes;
    private final String declaringClassName;

    private final Map<Type, FieldDescriptor> parameterTypeToField = new HashMap<>();

    ControllerAdviceAbstractExceptionMapperGenerator(MethodInfo controllerAdviceMethod, DotName exceptionDotName,
            ClassOutput classOutput, TypesUtil typesUtil) {
        super(exceptionDotName, classOutput);
        this.controllerAdviceMethod = controllerAdviceMethod;
        this.typesUtil = typesUtil;

        this.returnType = controllerAdviceMethod.returnType();
        this.parameterTypes = controllerAdviceMethod.parameters();
        this.declaringClassName = controllerAdviceMethod.declaringClass().name().toString();
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
    }

    @Override
    void generateMethodBody(MethodCreator toResponse) {
        if (returnType.kind() == Type.Kind.VOID) {
            AnnotationInstance responseStatusInstance = controllerAdviceMethod.annotation(RESPONSE_STATUS);

            // invoke the @ExceptionHandler method
            exceptionHandlerMethodResponse(toResponse);

            // build a JAX-RS response
            ResultHandle status = toResponse
                    .load(responseStatusInstance != null ? getHttpStatusFromAnnotation(responseStatusInstance)
                            : Response.Status.NO_CONTENT.getStatusCode());
            ResultHandle responseBuilder = toResponse.invokeStaticMethod(
                    MethodDescriptor.ofMethod(Response.class, "status", Response.ResponseBuilder.class, int.class),
                    status);

            ResultHandle httpResponseType = toResponse.load("text/plain");
            toResponse.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(Response.ResponseBuilder.class, "type", Response.ResponseBuilder.class,
                            String.class),
                    responseBuilder, httpResponseType);

            ResultHandle response = toResponse.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(Response.ResponseBuilder.class, "build", Response.class),
                    responseBuilder);
            toResponse.returnValue(response);
        } else {
            ResultHandle exceptionHandlerMethodResponse = exceptionHandlerMethodResponse(toResponse);

            ResultHandle response;
            if (RESPONSE_ENTITY.equals(returnType.name())) {
                // convert Spring's ResponseEntity to JAX-RS Response
                response = toResponse.invokeStaticMethod(
                        MethodDescriptor.ofMethod(ResponseEntityConverter.class.getName(), "toResponse",
                                Response.class.getName(), RESPONSE_ENTITY.toString()),
                        exceptionHandlerMethodResponse);
            } else {
                ResultHandle status = toResponse.load(getStatus(controllerAdviceMethod.annotation(RESPONSE_STATUS)));

                ResultHandle responseBuilder = toResponse.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Response.class, "status", Response.ResponseBuilder.class, int.class),
                        status);

                toResponse.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(Response.ResponseBuilder.class, "entity", Response.ResponseBuilder.class,
                                Object.class),
                        responseBuilder, exceptionHandlerMethodResponse);

                response = toResponse.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(Response.ResponseBuilder.class, "build", Response.class),
                        responseBuilder);
            }

            toResponse.returnValue(response);
        }
    }

    private ResultHandle exceptionHandlerMethodResponse(MethodCreator toResponse) {
        String returnTypeClassName = returnType.kind() == Type.Kind.VOID ? void.class.getName() : returnType.name().toString();

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
        ResultHandle controllerAdviceClass = toResponse.invokeStaticMethod(
                MethodDescriptor.ofMethod(Class.class, "forName", Class.class, String.class),
                toResponse.load(declaringClassName));

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

    private int getStatus(AnnotationInstance instance) {
        if (instance == null) {
            return 200;
        }
        return getHttpStatusFromAnnotation(instance);
    }
}
