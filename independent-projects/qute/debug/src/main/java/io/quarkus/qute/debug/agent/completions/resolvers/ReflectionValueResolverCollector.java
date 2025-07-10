package io.quarkus.qute.debug.agent.completions.resolvers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.lsp4j.debug.CompletionItem;
import org.eclipse.lsp4j.debug.CompletionItemType;

import io.quarkus.qute.EvalContext;
import io.quarkus.qute.ValueResolver;
import io.quarkus.qute.debug.agent.completions.CompletionContext;

public class ReflectionValueResolverCollector implements ResolverCollector {

    @Override
    public String getClassName() {
        return "io.quarkus.qute.ReflectionValueResolver";
    }

    @Override
    public boolean isApplicable(ValueResolver valueResolver, EvalContext evalContext) {
        return true;
    }

    @Override
    public void collect(CompletionContext context) {
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
            for (Method method : clazzToTest.getMethods()) {
                if (isMethodCandidate(method)) {
                    CompletionItem item = new CompletionItem();
                    item.setLabel(method.getName());
                    item.setType(CompletionItemType.METHOD);
                    context.add(item);
                }
            }

            for (Field field : clazz.getFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    CompletionItem item = new CompletionItem();
                    item.setLabel(field.getName());
                    item.setType(CompletionItemType.FIELD);
                    context.add(item);
                }
            }
        }
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
