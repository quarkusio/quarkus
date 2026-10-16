package io.quarkus.arc.processor;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.enterprise.invoke.AsyncHandler;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

public class InvokerFactory {
    private final BeanDeployment beanDeployment;
    private final InjectionPointModifier injectionPointTransformer;
    private final List<AsyncHandlerInfo> asyncHandlers;

    InvokerFactory(BeanDeployment beanDeployment, InjectionPointModifier injectionPointTransformer,
            List<Class<?>> configuredAsyncHandlers) {
        this.beanDeployment = beanDeployment;
        this.injectionPointTransformer = injectionPointTransformer;

        List<AsyncHandlerInfo> asyncHandlers = new ArrayList<>();
        Map<DotName, Set<DotName>> asyncHandlersByAsyncType = new HashMap<>();
        for (Class<?> handler : configuredAsyncHandlers) {
            AsyncHandlerInfo asyncHandlerInfo = createAsyncHandlerInfo(handler);
            asyncHandlers.add(asyncHandlerInfo);
            asyncHandlersByAsyncType.computeIfAbsent(asyncHandlerInfo.asyncType(), ignored -> new HashSet<>())
                    .add(asyncHandlerInfo.clazz().name());
        }
        Set<DotName> configured = Set.copyOf(asyncHandlersByAsyncType.keySet());
        List<Class<?>> discovered = new ArrayList<>();
        ServiceLoader.load(AsyncHandler.ReturnType.class)
                .stream()
                .map(ServiceLoader.Provider::type)
                .forEach(discovered::add);
        ServiceLoader.load(AsyncHandler.ParameterType.class)
                .stream()
                .map(ServiceLoader.Provider::type)
                .forEach(discovered::add);
        for (Class<?> asyncHandler : discovered) {
            AsyncHandlerInfo asyncHandlerInfo = createAsyncHandlerInfo(asyncHandler);
            if (configured.contains(asyncHandlerInfo.asyncType())) {
                continue;
            }
            asyncHandlers.add(asyncHandlerInfo);
            asyncHandlersByAsyncType.computeIfAbsent(asyncHandlerInfo.asyncType(), ignored -> new HashSet<>())
                    .add(asyncHandlerInfo.clazz().name());
        }
        asyncHandlersByAsyncType.forEach((type, handlers) -> {
            if (handlers.size() > 1) {
                StringBuilder error = new StringBuilder("Multiple async handlers defined for async type ")
                        .append(type).append(":\n");
                for (DotName handler : handlers) {
                    error.append("\t- ").append(handler).append("\n");
                }
                error.append("You have to configure the async handler class for this async type explicitly");
                throw new DeploymentException(error.toString());
            }
        });
        this.asyncHandlers = Collections.unmodifiableList(asyncHandlers);
    }

    private AsyncHandlerInfo createAsyncHandlerInfo(Class<?> asyncHandlerClass) {
        ClassInfo clazz;
        try {
            clazz = Index.singleClass(asyncHandlerClass);
        } catch (IOException e) {
            throw new DeploymentException(e);
        }

        DotName asyncType = null;
        boolean returnType = false;
        boolean parameterType = false;
        for (Type iface : clazz.interfaceTypes()) {
            if (DotNames.ASYNC_HANDLER_RETURN_TYPE.equals(iface.name())) {
                returnType = true;
            }
            if (DotNames.ASYNC_HANDLER_PARAMETER_TYPE.equals(iface.name())) {
                parameterType = true;
            }
            if (DotNames.ASYNC_HANDLER_RETURN_TYPE.equals(iface.name())
                    || DotNames.ASYNC_HANDLER_PARAMETER_TYPE.equals(iface.name())) {
                if (iface.kind() != Type.Kind.PARAMETERIZED_TYPE) {
                    throw new DefinitionException("Raw superinterface at async handler " + clazz);
                }
                Type typeArg = iface.asParameterizedType().arguments().get(0);
                if (typeArg.kind() != Type.Kind.CLASS && typeArg.kind() != Type.Kind.PARAMETERIZED_TYPE) {
                    throw new DefinitionException("Invalid type argument to async handler " + clazz
                            + " superinterface: " + typeArg);
                }
                asyncType = typeArg.name();
            }
        }
        if (returnType && parameterType) {
            throw new DefinitionException("Async handler " + clazz
                    + " implements both `AsyncHandler.ReturnType` and `AsyncHandler.ParameterType`");
        }
        if (!returnType && !parameterType) {
            throw new DefinitionException("Async handler " + clazz
                    + " implements neither `AsyncHandler.ReturnType` nor `AsyncHandler.ParameterType`\n"
                    + "Note that it must be a _direct_ superinterface type, inheriting it is not supported");
        }

        if (!Modifier.isPublic(clazz.flags())) {
            throw new DefinitionException("Async handler " + clazz + " is not public");
        }
        boolean hasPublicZeroParamCtor = false;
        for (MethodInfo method : clazz.methods()) {
            if (method.isConstructor() && method.descriptorParametersCount() == 0 && Modifier.isPublic(method.flags())) {
                hasPublicZeroParamCtor = true;
                break;
            }
        }
        if (!hasPublicZeroParamCtor) {
            throw new DefinitionException("Async handler " + clazz + " does not have a public zero-parameter constructor");
        }

        return new AsyncHandlerInfo(clazz, asyncType, returnType);
    }

    List<AsyncHandlerInfo> getAsyncHandlers() {
        return asyncHandlers;
    }

    public InvokerBuilder createInvoker(BeanInfo targetBean, MethodInfo targetMethod) {
        Objects.requireNonNull(targetBean);
        Objects.requireNonNull(targetMethod);

        if (!targetBean.isClassBean()) {
            throw new DeploymentException("Cannot build invoker for target bean: " + targetBean);
        }
        if (targetBean.isInterceptor() || targetBean.isDecorator()) {
            throw new DeploymentException("Cannot build invoker for target bean: " + targetBean);
        }
        if (targetMethod.isSynthetic()
                || targetMethod.isConstructor()
                || targetMethod.isStaticInitializer()
                || Modifier.isPrivate(targetMethod.flags())) {
            throw new DeploymentException("Cannot build invoker for target method: " + targetMethod);
        }
        if (DotNames.OBJECT.equals(targetMethod.declaringClass().name()) && !Methods.TO_STRING.equals(targetMethod.name())) {
            throw new DeploymentException("Cannot build invoker for target method: " + targetMethod);
        }
        // verify that the `targetMethod` belongs to the `targetBean`
        boolean isOwnMethod = false;
        IndexView index = beanDeployment.getBeanArchiveIndex();
        Deque<ClassInfo> worklist = new ArrayDeque<>();
        worklist.add(targetBean.getImplClazz());
        while (!worklist.isEmpty()) {
            ClassInfo clazz = worklist.poll();
            if (clazz.methods().contains(targetMethod)) {
                isOwnMethod = true;
                break;
            }
            DotName superClassName = clazz.superName();
            if (!DotNames.OBJECT.equals(superClassName)) {
                ClassInfo superClass = index.getClassByName(superClassName);
                worklist.add(superClass);
            }
            for (DotName superInterfaceName : clazz.interfaceNames()) {
                ClassInfo superInterface = index.getClassByName(superInterfaceName);
                worklist.add(superInterface);
            }
        }
        if (!isOwnMethod) {
            throw new DeploymentException("Method does not belong to target bean " + targetBean + ": " + targetMethod);
        }
        AsyncHandlerInfo asyncHandler = null;
        for (AsyncHandlerInfo candidate : asyncHandlers) {
            if (candidate.matches(targetMethod)) {
                if (asyncHandler == null) {
                    asyncHandler = candidate;
                } else {
                    // multiple matches, the invoker is _not_ async
                    asyncHandler = null;
                    break;
                }
            }
        }
        return new InvokerBuilder(targetBean, targetMethod, asyncHandler, beanDeployment, injectionPointTransformer);
    }
}
