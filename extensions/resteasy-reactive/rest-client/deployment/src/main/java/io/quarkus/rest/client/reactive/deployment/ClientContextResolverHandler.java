package io.quarkus.rest.client.reactive.deployment;

import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;

import jakarta.ws.rs.Priorities;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.GenericType;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.TypeArgument;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.rest.client.reactive.runtime.ResteasyReactiveContextResolver;
import io.quarkus.runtime.util.HashUtil;

/**
 * Generates an implementation of {@link ResteasyReactiveContextResolver}
 *
 * The extension will search for methods annotated with a special annotation like `@ClientObjectMapper` (if the REST Client
 * Jackson extension is present) and create the context resolver to register a custom object into the client context like the
 * ObjectMapper instance.
 */
class ClientContextResolverHandler {

    private static final MethodDesc GET_INVOKED_METHOD = MethodDesc.of(RestClientRequestContext.class,
            "getInvokedMethod", Method.class);
    private static final MethodDesc GET_CONTEXT_CLASS_LOADER = MethodDesc.of(Thread.class,
            "getContextClassLoader", ClassLoader.class);
    private static final MethodDesc ARC_CONTAINER = MethodDesc.of(Arc.class,
            "container", ArcContainer.class);
    private static final MethodDesc ARC_CONTAINER_INSTANCE = MethodDesc.of(ArcContainer.class,
            "instance", InstanceHandle.class, Class.class, Annotation[].class);
    private static final MethodDesc INSTANCE_HANDLE_GET = MethodDesc.of(InstanceHandle.class,
            "get", Object.class);

    private final DotName annotation;
    private final Class<?> expectedReturnType;
    private final Gizmo gizmo;

    ClientContextResolverHandler(DotName annotation, Class<?> expectedReturnType, Gizmo gizmo) {
        this.annotation = annotation;
        this.expectedReturnType = expectedReturnType;
        this.gizmo = gizmo;
    }

    /**
     * Generates an implementation of {@link ResteasyReactiveContextResolver} that looks something like:
     *
     * <pre>
     * {@code
     *  public class SomeService_map_ContextResolver_a8fb70beeef2a54b80151484d109618eed381626
     *      implements ResteasyReactiveContextResolver<T> {
     *
     *      public T getContext(Class<?> type) {
     *          // simply call the static method of interface
     *          return SomeService.map(var1);
     *      }
     *
     * }
     * </pre>
     */
    GeneratedClassResult generateContextResolver(AnnotationInstance instance) {
        if (!annotation.equals(instance.name())) {
            throw new IllegalArgumentException(
                    "'clientContextResolverInstance' must be an instance of " + annotation);
        }
        MethodInfo targetMethod = findTargetMethod(instance);
        if (targetMethod == null) {
            return null;
        }

        int priority = Priorities.USER;
        AnnotationValue priorityAnnotationValue = instance.value("priority");
        if (priorityAnnotationValue != null) {
            priority = priorityAnnotationValue.asInt();
        }

        Class<?> returnTypeClassName = lookupReturnClass(targetMethod);
        if (!expectedReturnType.isAssignableFrom(returnTypeClassName)) {
            throw new IllegalStateException(annotation
                    + " is only supported on static methods of REST Client interfaces that return '" + expectedReturnType + "'."
                    + " Offending instance is '" + targetMethod.declaringClass().name().toString() + "#"
                    + targetMethod.name() + "'");
        }

        ClassInfo restClientInterfaceClassInfo = targetMethod.declaringClass();
        String generatedClassName = getGeneratedClassName(targetMethod);
        final MethodInfo target = targetMethod;
        gizmo.class_(generatedClassName, cc -> {
            cc.implements_(GenericType.ofClass(ResteasyReactiveContextResolver.class,
                    TypeArgument.of(returnTypeClassName)));
            cc.defaultConstructor();
            cc.method("getContext", mc -> {
                mc.returning(Object.class);
                ParamVar typeParam = mc.parameter("type", Class.class);
                mc.body(bc -> {
                    LinkedHashMap<String, Expr> targetMethodParams = new LinkedHashMap<>();
                    for (Type paramType : target.parameterTypes()) {
                        Expr targetMethodParamHandle;
                        if (paramType.name().equals(DotNames.METHOD)) {
                            targetMethodParamHandle = bc.invokeVirtual(GET_INVOKED_METHOD, typeParam);
                        } else {
                            targetMethodParamHandle = getFromCDI(bc, target.returnType().name().toString());
                        }

                        targetMethodParams.put(paramType.name().toString(), targetMethodParamHandle);
                    }

                    Expr resultHandle = bc.invokeStatic(methodDescOf(target),
                            targetMethodParams.values().toArray(new Expr[0]));
                    bc.return_(resultHandle);
                });
            });
        });

        return new GeneratedClassResult(restClientInterfaceClassInfo.name().toString(), generatedClassName, priority);
    }

    private MethodInfo findTargetMethod(AnnotationInstance instance) {
        MethodInfo targetMethod = null;
        if (instance.target().kind() == AnnotationTarget.Kind.METHOD) {
            targetMethod = instance.target().asMethod();
            if (ignoreAnnotation(targetMethod)) {
                return null;
            }
            if ((targetMethod.flags() & Modifier.STATIC) != 0) {
                if (targetMethod.returnType().kind() == Type.Kind.VOID) {
                    throw new IllegalStateException(annotation
                            + " is only supported on static methods of REST Client interfaces that return an object."
                            + " Offending instance is '" + targetMethod.declaringClass().name().toString() + "#"
                            + targetMethod.name() + "'");
                }

            }
        }

        return targetMethod;
    }

    private static Class<?> lookupReturnClass(MethodInfo targetMethod) {
        Class<?> returnTypeClassName = null;
        try {
            returnTypeClassName = Class.forName(targetMethod.returnType().name().toString(), false,
                    Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException ignored) {

        }
        return returnTypeClassName;
    }

    private static Expr getFromCDI(BlockCreator bc, String className) {
        Expr containerHandle = bc.invokeStatic(ARC_CONTAINER);
        Expr currentThread = bc.currentThread();
        Expr tccl = bc.invokeVirtual(GET_CONTEXT_CLASS_LOADER, currentThread);
        Expr classHandle = bc.classForName(Const.of(className), Const.of(false), tccl);
        Expr instanceHandle = bc.invokeInterface(ARC_CONTAINER_INSTANCE,
                containerHandle, classHandle,
                bc.newArray(Annotation.class));
        return bc.invokeInterface(INSTANCE_HANDLE_GET, instanceHandle);
    }

    public static String getGeneratedClassName(MethodInfo methodInfo) {
        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(methodInfo.name()).append("_").append(methodInfo.returnType().name().toString());
        for (Type i : methodInfo.parameterTypes()) {
            sigBuilder.append(i.name().toString());
        }

        return methodInfo.declaringClass().name().toString() + "_" + methodInfo.name() + "_"
                + "ContextResolver" + "_" + HashUtil.sha1(sigBuilder.toString());
    }

    private static boolean ignoreAnnotation(MethodInfo methodInfo) {
        // ignore the annotation if it's placed on a Kotlin companion class
        // this is not a problem since the Kotlin compiler will also place the annotation the static method interface method
        return methodInfo.declaringClass().name().toString().contains("$Companion");
    }
}
