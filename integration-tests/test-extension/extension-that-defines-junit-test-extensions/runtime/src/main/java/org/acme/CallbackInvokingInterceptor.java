package org.acme;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class CallbackInvokingInterceptor implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {

        Class testClass = context.getRequiredTestClass();

        // Find everything annotated @Callback
        List<Method> callbacks = getMethodsAnnotatedWith(testClass, Callback.class);
        for (Method m : callbacks) {
            m.invoke(context.getRequiredTestInstance());
        }

    }

    protected static List<Method> getMethodsAnnotatedWith(Class<?> clazz, final Class<? extends Annotation> annotationClass) {
        final List<Method> methods = new ArrayList<Method>();
        while (clazz != Object.class) {
            for (final Method method : clazz.getDeclaredMethods()) {
                // Check by name since we could have some classloader mismatches
                Annotation[] allAnnotations = method.getAnnotations();
                for (Annotation annotation : allAnnotations) {
                    if (annotation.annotationType().getName().equals(annotationClass.getName()))
                        methods.add(method);
                }
            }
            // move to the upper class in the hierarchy in search for more methods
            clazz = clazz.getSuperclass();
        }
        return methods;
    }
}
