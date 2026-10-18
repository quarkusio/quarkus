package io.quarkus.rest.client.reactive.deployment;

import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;

import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.rest.client.reactive.runtime.ResteasyReactiveResponseExceptionMapper;
import io.quarkus.runtime.util.HashUtil;

/**
 * Generates an implementation of {@link org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper}
 * from an instance of {@link io.quarkus.rest.client.reactive.ClientExceptionMapper}
 */
class ClientExceptionMapperHandler {

    private static final MethodDesc GET_INVOKED_METHOD = MethodDesc.of(RestClientRequestContext.class,
            "getInvokedMethod", Method.class);
    private static final MethodDesc GET_URI = MethodDesc.of(RestClientRequestContext.class, "getUri",
            URI.class);
    private static final MethodDesc GET_PROPERTIES = MethodDesc.of(RestClientRequestContext.class,
            "getProperties", Map.class);
    private static final MethodDesc GET_REQUEST_HEADERS_AS_MAP = MethodDesc.of(RestClientRequestContext.class,
            "getRequestHeadersAsMap", MultivaluedMap.class);
    private final Gizmo gizmo;

    ClientExceptionMapperHandler(Gizmo gizmo) {
        this.gizmo = gizmo;
    }

    /**
     * Generates an implementation of {@link ResponseExceptionMapper} that looks something like:
     *
     * <pre>
     * {@code
     *  public class SomeService_map_ResponseExceptionMapper_a8fb70beeef2a54b80151484d109618eed381626 implements ResteasyReactiveResponseExceptionMapper {
     *      public Throwable toThrowable(Response var1, RestClientRequestContext var2) {
     *          // simply call the static method of interface
     *          return SomeService.map(var1);
     *      }
     *
     * }
     * </pre>
     */
    GeneratedClassResult generateResponseExceptionMapper(AnnotationInstance instance) {
        if (!DotNames.CLIENT_EXCEPTION_MAPPER.equals(instance.name())) {
            throw new IllegalArgumentException(
                    "'clientExceptionMapperInstance' must be an instance of " + DotNames.CLIENT_EXCEPTION_MAPPER);
        }
        MethodInfo targetMethod = null;
        boolean isValid = false;
        if (instance.target().kind() == AnnotationTarget.Kind.METHOD) {
            targetMethod = instance.target().asMethod();
            if (ignoreAnnotation(targetMethod)) {
                return null;
            }
            if ((targetMethod.flags() & Modifier.STATIC) != 0) {
                String returnTypeClassName = targetMethod.returnType().name().toString();
                try {
                    boolean returnsRuntimeException = RuntimeException.class.isAssignableFrom(
                            Class.forName(returnTypeClassName, false, Thread.currentThread().getContextClassLoader()));
                    if (returnsRuntimeException) {
                        isValid = true;
                    }
                } catch (ClassNotFoundException ignored) {

                }
            }
        }
        if (!isValid) {
            String message = DotNames.CLIENT_EXCEPTION_MAPPER
                    + " is only supported on static methods of REST Client interfaces that take 'jakarta.ws.rs.core.Response' as a single parameter and return 'java.lang.RuntimeException'.";
            if (targetMethod != null) {
                message += " Offending instance is '" + targetMethod.declaringClass().name().toString() + "#"
                        + targetMethod.name() + "'";
            }
            throw new IllegalStateException(message);
        }

        int priority = Priorities.USER;
        AnnotationValue priorityAnnotationValue = instance.value("priority");
        if (priorityAnnotationValue != null) {
            priority = priorityAnnotationValue.asInt();
        }

        ClassInfo restClientInterfaceClassInfo = targetMethod.declaringClass();
        String generatedClassName = getGeneratedClassName(targetMethod);
        final MethodInfo target = targetMethod;
        final int finalPriority = priority;
        gizmo.class_(generatedClassName, cc -> {
            cc.implements_(ResteasyReactiveResponseExceptionMapper.class);
            cc.defaultConstructor();
            cc.method("toThrowable", mc -> {
                mc.returning(Throwable.class);
                ParamVar response = mc.parameter("response", Response.class);
                ParamVar requestContext = mc.parameter("requestContext", RestClientRequestContext.class);
                mc.body(bc -> {
                    LinkedHashMap<String, Expr> targetMethodParams = new LinkedHashMap<>();
                    for (Type paramType : target.parameterTypes()) {
                        Expr targetMethodParamHandle;
                        DotName paramTypeName = paramType.name();
                        if (paramTypeName.equals(ResteasyReactiveDotNames.RESPONSE)) {
                            targetMethodParamHandle = response;
                        } else if (paramTypeName.equals(DotNames.METHOD)) {
                            targetMethodParamHandle = bc.invokeVirtual(GET_INVOKED_METHOD, requestContext);
                        } else if (paramTypeName.equals(DotNames.URI)) {
                            targetMethodParamHandle = bc.invokeVirtual(GET_URI, requestContext);
                        } else if (isMapStringToObject(paramType)) {
                            targetMethodParamHandle = bc.invokeVirtual(GET_PROPERTIES, requestContext);
                        } else if (isMultivaluedMapStringToString(paramType)) {
                            targetMethodParamHandle = bc.invokeVirtual(GET_REQUEST_HEADERS_AS_MAP, requestContext);
                        } else {
                            String message = "Unsupported parameter type used in " + DotNames.CLIENT_EXCEPTION_MAPPER
                                    + ". See the Javadoc of the annotation for the supported types."
                                    + " Offending instance is '" + target.declaringClass().name().toString() + "#"
                                    + target.name() + "'";
                            throw new IllegalStateException(message);
                        }
                        targetMethodParams.put(paramTypeName.toString(), targetMethodParamHandle);
                    }

                    Expr resultHandle = bc.invokeStatic(methodDescOf(target),
                            targetMethodParams.values().toArray(new Expr[0]));
                    bc.return_(resultHandle);
                });
            });

            if (finalPriority != Priorities.USER) {
                cc.method("getPriority", mc -> {
                    mc.returning(int.class);
                    mc.body(bc -> {
                        bc.return_(Const.of(finalPriority));
                    });
                });
            }
        });

        return new GeneratedClassResult(restClientInterfaceClassInfo.name().toString(), generatedClassName, priority);
    }

    private boolean isMapStringToObject(Type paramType) {
        if (paramType.kind() != Type.Kind.PARAMETERIZED_TYPE) {
            return false;
        }
        ParameterizedType parameterizedType = paramType.asParameterizedType();
        if (!parameterizedType.name().equals(DotNames.MAP)) {
            return false;
        }
        List<Type> arguments = parameterizedType.arguments();
        if (arguments.size() != 2) {
            return false;
        }
        return arguments.get(0).name().equals(DotNames.STRING) && arguments.get(1).name().equals(DotNames.OBJECT);
    }

    private boolean isMultivaluedMapStringToString(Type paramType) {
        if (paramType.kind() != Type.Kind.PARAMETERIZED_TYPE) {
            return false;
        }
        ParameterizedType parameterizedType = paramType.asParameterizedType();
        if (!parameterizedType.name().equals(DotNames.MULTIVALUED_MAP)) {
            return false;
        }
        List<Type> arguments = parameterizedType.arguments();
        if (arguments.size() != 2) {
            return false;
        }
        return arguments.get(0).name().equals(DotNames.STRING) && arguments.get(1).name().equals(DotNames.STRING);
    }

    public static String getGeneratedClassName(MethodInfo methodInfo) {
        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(methodInfo.name()).append("_").append(methodInfo.returnType().name().toString());
        for (Type i : methodInfo.parameterTypes()) {
            sigBuilder.append(i.name().toString());
        }

        return methodInfo.declaringClass().name().toString() + "_" + methodInfo.name() + "_"
                + "ResponseExceptionMapper" + "_" + HashUtil.sha1(sigBuilder.toString());
    }

    private static boolean ignoreAnnotation(MethodInfo methodInfo) {
        // ignore the annotation if it's placed on a Kotlin companion class
        // this is not a problem since the Kotlin compiler will also place the annotation the static method interface method
        return methodInfo.declaringClass().name().toString().contains("$Companion");
    }
}
