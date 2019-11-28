package io.quarkus.deployment.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

/**
 * A factory that can generate proxies of a class.
 * This was inspired from jboss-invocations's org.jboss.invocation.proxy.ProxyFactory
 */
public class ProxyFactory<T> {

    private final String proxyName;
    private final ClassLoader classLoader;

    private final String superClassName;
    private final List<Method> methods;
    private final ClassCreator.Builder classBuilder;

    private boolean classDefined = false;
    private final Object lock = new Object();

    public ProxyFactory(ProxyConfiguration<T> configuration) {
        Objects.requireNonNull(configuration.getAnchorClass(), "anchorClass must be set");
        Objects.requireNonNull(configuration.getProxyNameSuffix(), "proxyNameSuffix must be set");
        this.proxyName = configuration.getProxyName();

        Class<T> superClass = configuration.getSuperClass() != null ? configuration.getSuperClass() : (Class<T>) Object.class;
        this.superClassName = superClass.getName();

        if (!configuration.isAllowPackagePrivate() && !Modifier.isPublic(superClass.getModifiers())) {
            throw new IllegalArgumentException(
                    "A proxy cannot be created for class " + this.superClassName + " because the it is not public");
        }
        if (!hasNoArgsConstructor(superClass, configuration.isAllowPackagePrivate())) {
            throw new IllegalArgumentException(
                    "A proxy cannot be created for class " + this.superClassName
                            + " because it does contain a no-arg constructor");
        }
        if (Modifier.isFinal(superClass.getModifiers())) {
            throw new IllegalArgumentException(
                    "A proxy cannot be created for class " + this.superClassName + " because it is a final class");
        }

        Objects.requireNonNull(configuration.getClassLoader(), "classLoader must be set");
        this.classLoader = configuration.getClassLoader();

        this.methods = new ArrayList<>(superClass.getMethods().length);
        addMethodsOfClass(superClass);
        for (Class<?> additionalInterface : configuration.getAdditionalInterfaces()) {
            addMethodsOfClass(additionalInterface);
        }

        this.classBuilder = ClassCreator.builder()
                .classOutput(configuration.getClassOutput() != null ? configuration.getClassOutput()
                        : new InjectIntoClassloaderClassOutput(configuration.getClassLoader()))
                .className(this.proxyName)
                .superClass(this.superClassName);
        if (!configuration.getAdditionalInterfaces().isEmpty()) {
            this.classBuilder.interfaces(configuration.getAdditionalInterfaces().toArray(new Class[0]));
        }
    }

    private boolean hasNoArgsConstructor(Class<?> clazz, boolean allowPackagePrivate) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == 0) {
                if (allowPackagePrivate) {
                    return !Modifier.isPrivate(constructor.getModifiers());
                }
                return Modifier.isPublic(constructor.getModifiers()) || Modifier.isProtected(constructor.getModifiers());
            }
        }
        return false;
    }

    private void addMethodsOfClass(Class<?> clazz) {
        addMethodsOfClass(clazz, new HashSet<>());
    }

    private void addMethodsOfClass(Class<?> clazz, Set<MethodKey> seen) {
        for (Method methodInfo : clazz.getDeclaredMethods()) {
            MethodKey key = new MethodKey(methodInfo.getReturnType(), methodInfo.getName(), methodInfo.getParameterTypes());
            if (seen.contains(key)) {
                continue;
            }
            seen.add(key);
            if (!Modifier.isStatic(methodInfo.getModifiers()) &&
                    !Modifier.isFinal(methodInfo.getModifiers()) &&
                    !methodInfo.getName().equals("<init>")) {
                methods.add(methodInfo);
            }
        }
        if (clazz.getSuperclass() != null) {
            addMethodsOfClass(clazz.getSuperclass(), seen);
        }
    }

    public Class<? extends T> defineClass() {
        synchronized (lock) {
            if (!classDefined) {
                doDefineClass();
                classDefined = true;
            }
        }
        return loadClass();
    }

    private void doDefineClass() {
        try (ClassCreator cc = classBuilder.build()) {
            FieldDescriptor invocationHandlerField = cc.getFieldCreator("invocationHandler", InvocationHandler.class)
                    .setModifiers(Modifier.PRIVATE).getFieldDescriptor();

            try (MethodCreator ctor = cc.getMethodCreator(MethodDescriptor.ofConstructor(proxyName))) {
                ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(this.superClassName), ctor.getThis());
                ctor.writeInstanceField(invocationHandlerField, ctor.getThis(), ctor.loadNull());
                ctor.returnValue(null);
            }

            try (MethodCreator ctor = cc
                    .getMethodCreator(MethodDescriptor.ofConstructor(proxyName, InvocationHandler.class.getName()))) {
                ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(this.superClassName), ctor.getThis());
                ctor.writeInstanceField(invocationHandlerField, ctor.getThis(), ctor.getMethodParam(0));
                ctor.returnValue(null);
            }

            // proxy each method by forwarding to InvocationHandler
            for (Method methodInfo : methods) {
                try (MethodCreator mc = cc.getMethodCreator(toMethodDescriptor(methodInfo)).setModifiers(Modifier.PUBLIC)) {

                    // method = clazz.getDeclaredMethod(...)

                    ResultHandle getDeclaredMethodParamsArray = mc.newArray(Class.class,
                            mc.load(methodInfo.getParameterCount()));
                    for (int i = 0; i < methodInfo.getParameterCount(); i++) {
                        ResultHandle paramClass = mc.loadClass(methodInfo.getParameters()[i].getType());
                        mc.writeArrayValue(getDeclaredMethodParamsArray, i, paramClass);
                    }
                    ResultHandle method = mc.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(Class.class, "getDeclaredMethod", Method.class, String.class,
                                    Class[].class),
                            mc.loadClass(methodInfo.getDeclaringClass()), mc.load(methodInfo.getName()),
                            getDeclaredMethodParamsArray);

                    // result = invocationHandler.invoke(...)

                    ResultHandle invokeParamsArray = mc.newArray(Object.class, mc.load(methodInfo.getParameterCount()));
                    for (int i = 0; i < methodInfo.getParameterCount(); i++) {
                        mc.writeArrayValue(invokeParamsArray, i, mc.getMethodParam(i));
                    }
                    ResultHandle result = mc.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(InvocationHandler.class, "invoke", Object.class, Object.class,
                                    Method.class,
                                    Object[].class),
                            mc.readInstanceField(invocationHandlerField, mc.getThis()), mc.getThis(), method,
                            invokeParamsArray);

                    if (void.class.equals(methodInfo.getReturnType())) {
                        mc.returnValue(null);
                    } else {
                        mc.returnValue(result);
                    }

                }
            }
        }
    }

    private MethodDescriptor toMethodDescriptor(Method methodInfo) {
        final List<String> parameterTypesStr = new ArrayList<>();
        for (Parameter parameter : methodInfo.getParameters()) {
            parameterTypesStr.add(parameter.getType().getName());
        }
        return MethodDescriptor.ofMethod(proxyName, methodInfo.getName(), methodInfo.getReturnType(),
                parameterTypesStr.toArray(new Object[0]));
    }

    public T newInstance(InvocationHandler handler) throws IllegalAccessException, InstantiationException {
        synchronized (lock) {
            try {
                return defineClass().getConstructor(InvocationHandler.class).newInstance(handler);
            } catch (NoSuchMethodException | InvocationTargetException e) {
                // if this happens, we have not created the proxy correctly
                throw new IllegalStateException(e);
            }
        }

    }

    private Class<? extends T> loadClass() {
        try {
            return (Class<? extends T>) classLoader.loadClass(proxyName);
        } catch (ClassNotFoundException e) {
            // if this happens, we have not "written" the proxy to the class loader correctly
            throw new IllegalStateException(e);
        }
    }

    static class MethodKey {
        final Class<?> returnType;
        final String name;
        final Class<?>[] params;

        MethodKey(Class<?> returnType, String name, Class<?>[] params) {
            this.returnType = returnType;
            this.name = name;
            this.params = params;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            MethodKey methodKey = (MethodKey) o;
            return Objects.equals(returnType, methodKey.returnType) &&
                    Objects.equals(name, methodKey.name) &&
                    Arrays.equals(params, methodKey.params);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(returnType, name);
            result = 31 * result + Arrays.hashCode(params);
            return result;
        }
    }
}
