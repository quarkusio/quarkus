package org.jboss.protean.gizmo;

import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;

//TODO: should not be public
public class DescriptorUtils {

    private static final Class<?>[] PRIMITIVES = {
            byte.class,
            boolean.class,
            char.class,
            short.class,
            int.class,
            long.class,
            float.class,
            double.class,
            void.class
    };

    public static String methodSignitureToDescriptor(String returnType, String... params) {
        StringBuilder sb = new StringBuilder("(");
        for (String i : params) {
            sb.append(i);
        }
        sb.append(")");
        sb.append(returnType);
        return sb.toString();
    }

    /**
     * e.g. Ljava/lang/Object; to java/lang/Object
     */
    public static String getTypeStringFromDescriptorFormat(String descriptor) {
        if(descriptor.startsWith("[")) {
            return descriptor;
        }
        descriptor = descriptor.substring(1);
        descriptor = descriptor.substring(0, descriptor.length() - 1);
        return descriptor;
    }

    public static String classToStringRepresentation(Class<?> c) {
        if (void.class.equals(c)) {
            return "V";
        } else if (byte.class.equals(c)) {
            return "B";
        } else if (char.class.equals(c)) {
            return "C";
        } else if (double.class.equals(c)) {
            return "D";
        } else if (float.class.equals(c)) {
            return "F";
        } else if (int.class.equals(c)) {
            return "I";
        } else if (long.class.equals(c)) {
            return "J";
        } else if (short.class.equals(c)) {
            return "S";
        } else if (boolean.class.equals(c)) {
            return "Z";
        } else if (c.isArray()) {
            return c.getName().replace(".", "/");
        } else {
            return extToInt(c.getName());
        }
    }

    public static String extToInt(String className) {
        String repl = className.replace(".", "/");
        return 'L' + repl + ';';
    }

    public static boolean isPrimitive(String descriptor) {
        if (descriptor.length() == 1) {
            return true;
        }
        return false;
    }

    public static boolean isWide(String descriptor) {
        if (!isPrimitive(descriptor)) {
            return false;
        }
        char c = descriptor.charAt(0);
        if (c == 'D' || c == 'J') {
            return true;
        }
        return false;
    }

    /**
     * Coerces an object into a descriptor in the JVM internal format.
     * <p>
     * It accepts class and String parameters. If the parameter is a string it accepts:
     * - Standard JVM class names
     * - Internal Descriptors
     * - Primitive names as expressed in java (e.g. 'int')
     *
     * @param param The param
     * @return A descriptor
     */
    public static String objectToDescriptor(Object param) {
        if (param instanceof String) {
            String s = (String) param;
            if (s.length() == 1) {
                return s; //primitive
            }
            if (s.startsWith("[")) {
                return s.replace(".", "/");
            }
            if (s.endsWith(";")) {
                //jvm internal name
                return s;
            }
            for (Class<?> prim : PRIMITIVES) {
                if (s.equals(prim.getName())) {
                    return classToStringRepresentation(prim);
                }
            }
            return "L" + s.replace(".", "/") + ";";
        } else if (param instanceof Class) {
            return classToStringRepresentation((Class<?>) param);
        }
        throw new IllegalArgumentException("Must be a Class or String, got " + param);
    }

    /**
     * Array version of {@link #objectToDescriptor(Object)}
     *
     * @param param
     * @return
     */
    public static String[] objectsToDescriptor(Object[] param) {
        String[] ret = new String[param.length];
        for (int i = 0; i < param.length; ++i) {
            ret[i] = objectToDescriptor(param[i]);
        }
        return ret;
    }

    public static String objectToInternalClassName(Object param) {
        if (param instanceof String) {
            String s = (String) param;
            return s.replace(".", "/");
        } else if (param instanceof Class) {
            return ((Class) param).getName().replace(".", "/");
        }
        throw new IllegalArgumentException("Must be a Class or String, got " + param);
    }

    public static String typeToString(Type type) {
        if (type.kind() == Type.Kind.PRIMITIVE) {
            PrimitiveType.Primitive primitive = type.asPrimitiveType().primitive();
            switch (primitive) {
                case INT:
                    return "I";
                case BYTE:
                    return "B";
                case CHAR:
                    return "C";
                case LONG:
                    return "J";
                case FLOAT:
                    return "F";
                case SHORT:
                    return "S";
                case DOUBLE:
                    return "D";
                case BOOLEAN:
                    return "Z";
                default:
                    throw new RuntimeException("Unkown primitive type " + primitive);
            }
        } else if (type.kind() == Type.Kind.VOID) {
            return "V";
        } else if (type.kind() == Type.Kind.ARRAY) {
            ArrayType array = type.asArrayType();
            return array.name().toString().replace(".", "/");
        } else if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType pt = type.asParameterizedType();
            StringBuilder ret = new StringBuilder();
            ret.append("L");
            ret.append(pt.name().toString().replace(".", "/"));
            ret.append(";");
            return ret.toString();
        } else if (type.kind() == Type.Kind.CLASS) {
            ClassType pt = type.asClassType();
            StringBuilder ret = new StringBuilder();
            ret.append("L");
            ret.append(pt.name().toString().replace(".", "/"));
            ret.append(";");
            return ret.toString();
        } else if (type.kind() == Type.Kind.TYPE_VARIABLE) {
            TypeVariable pt = type.asTypeVariable();
            StringBuilder ret = new StringBuilder();
            ret.append("L");
            ret.append(pt.name().toString().replace(".", "/"));
            ret.append(";");
            return ret.toString();
        } else {
            throw new RuntimeException("Invalid type for descriptor " + type);
        }
    }
}