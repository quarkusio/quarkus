package io.quarkus.arc.deployment.devui;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.smallrye.common.annotation.SuppressForbidden;

public class Name implements Comparable<Name> {

    public static Name from(DotName dotName) {
        return new Name(dotName.toString(), dotName.withoutPackagePrefix());
    }

    @SuppressForbidden(reason = "Type.toString() is what we need here")
    public static Name from(Type type) {
        return new Name(type.toString(), createSimpleName(type));
    }

    public static Name from(AnnotationInstance annotation) {
        return new Name(annotation.toString(false), createSimple(annotation));
    }

    private final String name;
    private final String simpleName;

    private Name(String name, String simpleName) {
        this.name = Objects.requireNonNull(name);
        this.simpleName = simpleName;
    }

    public Name(String name) {
        this(name, null);
    }

    public String getSimpleName() {
        return simpleName != null ? simpleName : name;
    }

    public String getName() {
        return name;
    }

    @SuppressForbidden(reason = "Type.toString() is what we need here, in the ARRAY case")
    static String createSimpleName(Type type) {
        switch (type.kind()) {
            case CLASS:
                return createSimple(type.name().toString());
            case PARAMETERIZED_TYPE:
                return createSimple(type.asParameterizedType());
            case ARRAY:
                Type component = type.asArrayType().constituent();
                if (component.kind() == Kind.CLASS) {
                    return createSimple(type.toString());
                }
            default:
                return null;
        }
    }

    static String createSimple(String name) {
        int lastDot = name.lastIndexOf('.');
        if (lastDot != -1) {
            return name.substring(lastDot + 1);
        }
        return name;
    }

    static String createSimple(ParameterizedType parameterizedType) {
        StringBuilder builder = new StringBuilder();
        builder.append(createSimple(parameterizedType.name().toString()));
        List<Type> args = parameterizedType.arguments();
        if (!args.isEmpty()) {
            builder.append('<');
            for (Iterator<Type> it = args.iterator(); it.hasNext();) {
                builder.append(createSimpleName(it.next()));
                if (it.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append('>');
        }
        return builder.toString();
    }

    static String createSimple(AnnotationInstance annotation) {
        StringBuilder builder = new StringBuilder("@").append(createSimple(annotation.name().toString()));
        List<AnnotationValue> values = annotation.values();
        if (!values.isEmpty()) {
            builder.append("(");
            for (Iterator<AnnotationValue> it = values.iterator(); it.hasNext();) {
                builder.append(it.next());
                if (it.hasNext()) {
                    builder.append(",");
                }
            }
            builder.append(')');
        }

        return builder.toString();
    }

    @Override
    public int compareTo(Name other) {
        // Quarkus classes should be last
        int result = Boolean.compare(isQuarkusClassName(), other.isQuarkusClassName());
        if (result != 0) {
            return result;
        }
        return name.compareTo(other.name);
    }

    @Override
    public String toString() {
        return name;
    }

    private boolean isQuarkusClassName() {
        return name.startsWith("io.quarkus");
    }
}
