package io.quarkus.deployment;

import static org.jboss.protean.gizmo.DescriptorUtils.isPrimitive;

import java.util.HashMap;
import java.util.Map;

import org.jboss.protean.gizmo.DescriptorUtils;
import org.jboss.protean.gizmo.FieldDescriptor;
import org.jboss.protean.gizmo.MethodDescriptor;

/**
 */
public final class AccessorFinder {
    private static final String JLO = "java/lang/Object";

    private final Map<FieldDescriptor, MethodDescriptor> setters = new HashMap<>();
    private final Map<FieldDescriptor, MethodDescriptor> getters = new HashMap<>();
    private final Map<MethodDescriptor, MethodDescriptor> ctors = new HashMap<>();

    public AccessorFinder() {
    }

    public synchronized MethodDescriptor getSetterFor(FieldDescriptor fieldDescriptor) {
        MethodDescriptor methodDescriptor = setters.get(fieldDescriptor);
        if (methodDescriptor != null)
            return methodDescriptor;
        final String declaringClass = fieldDescriptor.getDeclaringClass();
        final String accessorName = declaringClass + "$$accessor";
        final String rawFieldType = fieldDescriptor.getType();
        final boolean primitive = isPrimitive(rawFieldType);
        final String fieldType = primitive ? rawFieldType : DescriptorUtils.getTypeStringFromDescriptorFormat(rawFieldType);
        final String publicType = primitive ? fieldType : JLO;
        methodDescriptor = MethodDescriptor.ofMethod(accessorName, "set_" + fieldDescriptor.getName(), void.class, JLO,
                publicType);
        setters.put(fieldDescriptor, methodDescriptor);
        return methodDescriptor;
    }

    public synchronized MethodDescriptor getGetterFor(FieldDescriptor fieldDescriptor) {
        MethodDescriptor methodDescriptor = getters.get(fieldDescriptor);
        if (methodDescriptor != null)
            return methodDescriptor;
        final String declaringClass = fieldDescriptor.getDeclaringClass();
        final String accessorName = declaringClass + "$$accessor";
        final String rawFieldType = fieldDescriptor.getType();
        final boolean primitive = isPrimitive(rawFieldType);
        final String fieldType = primitive ? rawFieldType : DescriptorUtils.getTypeStringFromDescriptorFormat(rawFieldType);
        final String publicType = primitive ? fieldType : JLO;
        methodDescriptor = MethodDescriptor.ofMethod(accessorName, "get_" + fieldDescriptor.getName(), publicType, JLO);
        getters.put(fieldDescriptor, methodDescriptor);
        return methodDescriptor;
    }

    public synchronized MethodDescriptor getConstructorFor(MethodDescriptor ctor) {
        MethodDescriptor methodDescriptor = ctors.get(ctor);
        if (methodDescriptor != null)
            return methodDescriptor;
        if (!ctor.getName().equals("<init>"))
            throw new IllegalArgumentException("Parameter 'ctor' must be a valid constructor descriptor");
        final String declaringClass = ctor.getDeclaringClass();
        final String accessorName = declaringClass + "$$accessor";
        final String[] parameterTypes = ctor.getParameterTypes();
        final String[] publicParameterTypes = new String[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            publicParameterTypes[i] = isPrimitive(parameterTypes[i]) ? parameterTypes[i] : JLO;
        }
        StringBuilder b = new StringBuilder();
        for (final String parameterType : parameterTypes) {
            b.append('_').append(parameterType.replace('.', '_'));
        }
        String codedName = b.toString();

        methodDescriptor = MethodDescriptor.ofMethod(accessorName, "construct" + codedName, JLO, publicParameterTypes);
        ctors.put(ctor, methodDescriptor);
        return methodDescriptor;
    }
}
