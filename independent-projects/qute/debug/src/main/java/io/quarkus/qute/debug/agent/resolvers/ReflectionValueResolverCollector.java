package io.quarkus.qute.debug.agent.resolvers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.quarkus.qute.EvalContext;
import io.quarkus.qute.ValueResolver;

public class ReflectionValueResolverCollector implements ValueResolverCollector {

    @Override
    public boolean isApplicable(ValueResolver valueResolver, EvalContext evalContext) {
        return true;
    }

    @Override
    public void collect(ValueResolver valueResolver, ValueResolverContext context) {
        Class<?> clazz = context.getBase().getClass();
        List<Class<?>> hierarchy = new ArrayList<>();
        Collections.addAll(hierarchy, clazz.getInterfaces());
        Class<?> superClass = clazz.getSuperclass();
        while (superClass != null) {
            Collections.addAll(hierarchy, superClass.getInterfaces());
            superClass = superClass.getSuperclass();
        }
        hierarchy.add(clazz);

        for (Class<?> clazzToTest : hierarchy) {

            if (context.isCollectProperty()) {
                for (Field field : clazz.getFields()) {
                    if (isFieldCandidate(field)) {
                        context.addProperty(field);
                    }
                }
            }

            if (context.isCollectMethod()) {
                for (Method method : clazzToTest.getMethods()) {
                    if (isMethodCandidate(method)) {
                        context.addMethod(method);
                    }
                }
            }
        }
    }

    public static boolean isFieldCandidate(Field field) {
        return !Modifier.isStatic(field.getModifiers());
    }

    private static boolean isMethodCandidate(Method method) {
        return method != null && Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())
                && !method.getReturnType().equals(Void.TYPE) && !method.isBridge()
                && !Object.class.equals(method.getDeclaringClass());
    }

    private static boolean isMethodProperty(Method method) {
        return isMethodCandidate(method) && method.getParameterCount() == 0;
    }
}
