package io.quarkus.deployment.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.quarkus.deployment.util.ClassOutputUtil;
import io.quarkus.gizmo.ClassOutput;

/**
 * A Gizmo {@link ClassOutput} that is able to write the inject the bytecode directly into the classloader
 *
 * The {@link ClassLoader} passed to the constructor MUST contain a public visibleDefineClass method
 * This ensures that generating proxies works in any JDK version
 */
public class InjectIntoClassloaderClassOutput implements ClassOutput {

    private final ClassLoader classLoader;

    private final Method visibleDefineClassMethod;

    InjectIntoClassloaderClassOutput(ClassLoader classLoader) {
        this.classLoader = classLoader;

        try {
            visibleDefineClassMethod = classLoader.getClass().getDeclaredMethod("visibleDefineClass", String.class,
                    byte[].class, int.class, int.class);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException(
                    "Unable to initialize InjectIntoClassloaderClassOutput - Incorrect classloader (" + classLoader.getClass()
                            + ") usage detected");
        }
    }

    @Override
    public void write(String name, byte[] data) {
        if (System.getProperty("dumpClass") != null) {
            ClassOutputUtil.dumpClass(name, data);
        }
        try {
            visibleDefineClassMethod.invoke(classLoader, name.replace('/', '.'), data, 0, data.length);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static ClassOutput wrap(ClassOutput delegate) {
        InjectIntoClassloaderClassOutput injector = new InjectIntoClassloaderClassOutput(
                Thread.currentThread().getContextClassLoader());

        return new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                delegate.write(name, data);
                injector.write(name, data);
            }
        };
    }
}
