package io.quarkus.qute;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ReflectionValueResolver implements ValueResolver {

    /**
     * Lazy loading cache of lookup attempts (contains both hits and misses)
     */
    private final ConcurrentMap<MemberKey, Optional<MemberWrapper>> memberCache = new ConcurrentHashMap<>();

    private static final MemberWrapper ARRAY_GET_LENGTH = Array::getLength;

    public static final String GET_PREFIX = "get";
    public static final String IS_PREFIX = "is";

    @Override
    public int getPriority() {
        return -1;
    }

    @Override
    public boolean appliesTo(EvalContext context) {
        return context.getBase() != null;
    }

    @Override
    public CompletionStage<Object> resolve(EvalContext context) {

        Object base = context.getBase();
        MemberKey key = MemberKey.newInstance(base, context.getName());
        MemberWrapper wrapper = memberCache.computeIfAbsent(key, ReflectionValueResolver::findWrapper).orElse(null);

        if (wrapper == null) {
            return Results.NOT_FOUND;
        }

        try {
            return CompletableFuture.completedFuture(wrapper.getValue(base));
        } catch (Exception e) {
            throw new IllegalStateException("Reflection invocation error", e);
        }
    }

    public void clearMemberCache() {
        memberCache.clear();
    }

    private static Optional<MemberWrapper> findWrapper(MemberKey key) {

        if (key.getClazz().isArray()) {
            if (key.getName().equals("length")) {
                return Optional.of(ARRAY_GET_LENGTH);
            } else {
                return Optional.empty();
            }
        }

        Method foundMethod = findMethod(key.getClazz(), key.getName());

        if (foundMethod != null) {
            if (!foundMethod.isAccessible()) {
                foundMethod.setAccessible(true);
            }
            return Optional.of(new MethodWrapper(foundMethod));
        }

        // Find public field
        Field foundField = findField(key.getClazz(), key.getName());

        if (foundField != null) {
            if (!foundField.isAccessible()) {
                foundField.setAccessible(true);
            }
            return Optional.of(new FieldWrapper(foundField));
        }
        // Member not found
        return Optional.empty();
    }

    private static Method findMethod(Class<?> clazz, String name) {

        Objects.requireNonNull(clazz);
        Objects.requireNonNull(name);

        Method foundMatch = null;
        Method foundGetMatch = null;
        Method foundIsMatch = null;

        for (Method method : clazz.getMethods()) {

            if (!isMethodValid(method)) {
                continue;
            }

            if (method.isBridge()) {
                continue;
            }

            if (name.equals(method.getName())) {
                foundMatch = method;
            } else if (matchesPrefix(name, method.getName(),
                    GET_PREFIX)) {
                foundGetMatch = method;
            } else if (matchesPrefix(name, method.getName(),
                    IS_PREFIX)) {
                foundIsMatch = method;
            }
        }

        if (foundMatch == null) {
            foundMatch = (foundGetMatch != null ? foundGetMatch : foundIsMatch);
        }

        return foundMatch;
    }

    /**
     * Tries to find a public field with the given name on the given class.
     *
     * @param clazz
     * @param name
     * @return the found field or <code>null</code>
     */
    private static Field findField(Class<?> clazz, String name) {

        Objects.requireNonNull(clazz);
        Objects.requireNonNull(name);

        Field found = null;

        for (Field field : clazz.getFields()) {
            if (field.getName().equals(name)) {
                found = field;
            }
        }
        return found;
    }

    private static boolean isMethodValid(Method method) {
        return method != null && Modifier.isPublic(method.getModifiers())
                && method.getParameterTypes().length == 0
                && !method.getReturnType().equals(Void.TYPE)
                && !Object.class.equals(method.getDeclaringClass());
    }

    private static boolean matchesPrefix(String name, String methodName,
            String prefix) {
        return methodName.startsWith(prefix)
                && decapitalize(methodName.substring(prefix.length(), methodName.length())).equals(name);
    }

    static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

}
