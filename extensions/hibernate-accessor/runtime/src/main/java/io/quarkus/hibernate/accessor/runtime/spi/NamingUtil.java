package io.quarkus.hibernate.accessor.runtime.spi;

import java.lang.invoke.MethodType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * Builds runtime lookup keys matching the keys embedded in generated accessor methods.
 */
public final class NamingUtil {

    private NamingUtil() {
    }

    /**
     * Returns a field's name or a method's name followed by its JVM descriptor.
     * Method descriptors distinguish overloads, including methods with different return types.
     * Keys are resolved within the member's declaring class.
     */
    public static String memberKey(Member member) {
        if (member instanceof Method method) {
            return method.getName() + MethodType.methodType(method.getReturnType(), method.getParameterTypes())
                    .toMethodDescriptorString();
        }
        return member.getName();
    }

    /**
     * Returns the JVM constructor descriptor, with the parameter types followed by a void return type.
     */
    public static <T> String constructorDescriptor(java.lang.reflect.Constructor<T> constructor) {
        MethodType mt = MethodType.methodType(void.class, constructor.getParameterTypes());
        return mt.toMethodDescriptorString();
    }
}
