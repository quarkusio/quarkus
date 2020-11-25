/**
 *
 */
package org.jboss.resteasy.reactive.common.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility class that can convert a String value as a typed object.
 *
 * @author <a href="ryan@damnhandy.com">Ryan J. McDonough</a>
 * @version $Revision: $
 *
 *          TODO: Do we really need this?
 */
public final class TypeConverter {
    private static final String VALUE_OF_METHOD = "valueOf";

    /**
     * A map of primitive to objects.
     */
    private static final Map<Class<?>, Class<?>> PRIMITIVES;

    static {
        PRIMITIVES = new HashMap<Class<?>, Class<?>>();
        PRIMITIVES.put(int.class, Integer.class);
        PRIMITIVES.put(double.class, Double.class);
        PRIMITIVES.put(float.class, Float.class);
        PRIMITIVES.put(short.class, Short.class);
        PRIMITIVES.put(byte.class, Byte.class);
        PRIMITIVES.put(long.class, Long.class);
    }

    private TypeConverter() {

    }

    /**
     * A generic method that returns the {@link String} as the specified Java type.
     *
     * @param <T> the type to return
     * @param source the string value to convert
     * @param targetType target type
     * @return the object instance
     */
    @SuppressWarnings(value = "unchecked")
    public static <T> T getType(final Class<T> targetType, final String source) {
        // just return that source if it's a String
        if (String.class.equals(targetType)) {
            return targetType.cast(source);
        }
        /*
         * Dates are too complicated for this class.
         */
        if (Date.class.isAssignableFrom(targetType)) {
            throw new IllegalArgumentException("Date instances are not supported by this class.");
        }
        if (Character.class.equals(targetType)) {
            if (source.length() == 0)
                return targetType.cast(new Character('\0'));
            return targetType.cast(new Character(source.charAt(0)));
        }
        if (char.class.equals(targetType)) {
            Character c = null;
            if (source.length() == 0)
                c = new Character('\0');
            else
                c = new Character(source.charAt(0));
            try {
                return (T) Character.class.getMethod("charValue").invoke(c);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

        }
        T result;
        // boolean types need special handling
        if (Boolean.class.equals(targetType) || boolean.class.equals(targetType)) {
            Boolean booleanValue = getBooleanValue(source);
            if (Boolean.class.equals(targetType)) {
                return targetType.cast(booleanValue);
            } else {
                try {
                    return (T) Boolean.class.getMethod("booleanValue").invoke(booleanValue);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        try {
            result = getTypeViaValueOfMethod(source, targetType);
        } catch (NoSuchMethodException e) {
            result = getTypeViaStringConstructor(source, targetType);
        }
        return result;
    }

    /**
     * Tests if the class can safely be converted from a String to the
     * specified type.
     *
     * @param targetType the type to convert to
     * @return true if the class possesses either a "valueOf()" method or a constructor with a String
     *         parameter.
     */
    public static boolean isConvertable(final Class<?> targetType) {
        if (Boolean.class.equals(targetType)) {
            return true;
        }
        if (Character.class.equals(targetType)) {
            return true;
        }

        if (targetType.isPrimitive()) {
            return true;
        }
        try {
            targetType.getDeclaredMethod(VALUE_OF_METHOD, String.class);
            return true;
        } catch (NoSuchMethodException e) {
            try {
                targetType.getDeclaredConstructor(String.class);
                return true;
            }

            catch (NoSuchMethodException e1) {
                return false;
            }
        }
    }

    /**
     * <p>
     * Returns a Boolean value from a String. Unlike {@link Boolean#valueOf(String)}, this
     * method takes more String options. The following String values will return true:
     * </p>
     * <ul>
     * <li>Yes</li>
     * <li>Y</li>
     * <li>T</li>
     * <li>1</li>
     * </ul>
     * <p>
     * While the following values will return false:
     * </p>
     * <ul>
     * <li>No</li>
     * <li>N</li>
     * <li>F</li>
     * <li>0</li>
     * </ul>
     *
     * @param source source string
     * @return boolean value from string
     */
    public static Boolean getBooleanValue(final String source) {
        if ("Y".equalsIgnoreCase(source) || "T".equalsIgnoreCase(source)
                || "Yes".equalsIgnoreCase(source) || "1".equalsIgnoreCase(source)) {
            return Boolean.TRUE;
        } else if ("N".equals(source) || "F".equals(source) || "No".equals(source)
                || "0".equalsIgnoreCase(source)) {
            return Boolean.FALSE;
        }
        return Boolean.valueOf(source);
    }

    /**
     * @param <T> type
     * @param source source string
     * @param targetType target type
     * @return object instance of type T
     * @throws NoSuchMethodException if method was not found
     */
    @SuppressWarnings("unchecked")
    public static <T> T getTypeViaValueOfMethod(final String source, final Class<T> targetType)
            throws NoSuchMethodException {
        Class<?> actualTarget = targetType;
        /*
         * if this is a primitive type, use the Object class's "valueOf()"
         * method.
         */
        if (targetType.isPrimitive()) {
            actualTarget = PRIMITIVES.get(targetType);
        }
        T result = null;
        try {
            // if the type has a static "valueOf()" method, try and create the instance that way
            Method valueOf = actualTarget.getDeclaredMethod(VALUE_OF_METHOD, String.class);
            Object value = valueOf.invoke(null, source);
            if (actualTarget.equals(targetType) && targetType.isInstance(value)) {
                result = targetType.cast(value);
            }
            /*
             * handle the primitive case
             */
            else if (!actualTarget.equals(targetType) && actualTarget.isInstance(value)) {
                // because you can't use targetType.cast() with primitives.
                result = (T) value;
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * @param <T> type
     * @param source source string
     * @param targetType target type
     * @return object instance of type T
     * @throws IllegalArgumentException if not suitable constructor was found
     * @throws InstantiationException if the underlying constructor represents an abstract class
     * @throws IllegalAccessException if the underlying constructor is not accessible
     * @throws InvocationTargetException if the underlying constructor throws exception
     */
    private static <T> T getTypeViaStringConstructor(String source, Class<T> targetType) {
        T result = null;
        Constructor<T> c = null;

        try {
            c = targetType.getDeclaredConstructor(String.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException((targetType.getName() + " has no String constructor"), e);
        }

        try {
            result = c.newInstance(source);
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
