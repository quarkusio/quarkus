package io.quarkus.qute.debug.agent.resolvers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.quarkus.qute.EvalContext;
import io.quarkus.qute.ValueResolver;

/**
 * Reflection-based collector for value resolvers.
 * <p>
 * This collector inspects the base object's class using reflection and adds
 * public, non-static fields and methods to the {@link ValueResolverContext}.
 * It is analogous to {@link ValueResolver#getSupportedProperties()} and
 * {@link ValueResolver#getSupportedMethods()} but uses reflection instead.
 */
public class ReflectionValueResolverCollector implements ValueResolverCollector {

    @Override
    public boolean isApplicable(ValueResolver valueResolver, EvalContext evalContext) {
        return true;
    }

    /**
     * Collects fields and methods from the base object's class and its hierarchy.
     * <p>
     * Only adds fields if {@link ValueResolverContext#isCollectProperty()} returns true.
     * Only adds methods if {@link ValueResolverContext#isCollectMethod()} returns true.
     *
     * @param valueResolver the value resolver (ignored for reflection)
     * @param context the context to populate with fields and methods
     */
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

    /**
     * Checks if a field is a candidate to be collected.
     * Excludes static fields.
     */
    public static boolean isFieldCandidate(Field field) {
        return !Modifier.isStatic(field.getModifiers());
    }

    /**
     * Checks if a method is a candidate to be collected.
     * Excludes static, void, bridge, or Object-declared methods.
     */
    private static boolean isMethodCandidate(Method method) {
        return method != null && Modifier.isPublic(method.getModifiers())
                && !Modifier.isStatic(method.getModifiers())
                && !method.getReturnType().equals(Void.TYPE)
                && !method.isBridge()
                && !Object.class.equals(method.getDeclaringClass());
    }

}
