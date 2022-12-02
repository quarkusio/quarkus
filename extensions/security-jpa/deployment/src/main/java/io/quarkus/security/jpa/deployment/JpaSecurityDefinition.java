package io.quarkus.security.jpa.deployment;

import java.lang.reflect.Modifier;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class JpaSecurityDefinition {
    public static class FieldOrMethod {
        public final FieldInfo field;
        public final MethodInfo getter;

        public FieldOrMethod(FieldInfo field, MethodInfo getter) {
            this.field = field;
            this.getter = getter;
        }

        public AnnotationInstance annotation(DotName annotationName) {
            if (field != null) {
                return field.annotation(annotationName);
            }
            return getter.annotation(annotationName);
        }

        public String name() {
            if (field != null) {
                return field.name();
            }
            return JavaBeanUtil.getPropertyNameFromGetter(getter.name());
        }

        public ResultHandle readValue(BytecodeCreator methodCreator, AssignableResultHandle userVar) {
            // favour the getter
            if (getter != null) {
                return methodCreator.invokeVirtualMethod(MethodDescriptor.of(getter), userVar);
            }
            return methodCreator.readInstanceField(FieldDescriptor.of(field), userVar);
        }

        public Type type() {
            // FIXME: this doesn't work with generics-inherited types
            if (field != null) {
                return field.type();
            }
            return getter.returnType();
        }
    }

    public final FieldOrMethod username;
    public final FieldOrMethod password;
    public final FieldOrMethod roles;
    public final ClassInfo annotatedClass;

    public JpaSecurityDefinition(Index index,
            ClassInfo annotatedClass,
            boolean isPanache,
            AnnotationTarget usernameFieldOrMethod,
            AnnotationTarget passwordFieldOrMethod,
            AnnotationTarget rolesFieldOrMethod) {
        this.annotatedClass = annotatedClass;
        this.username = getFieldOrMethod(index, annotatedClass, usernameFieldOrMethod, isPanache);
        this.password = getFieldOrMethod(index, annotatedClass, passwordFieldOrMethod, isPanache);
        this.roles = getFieldOrMethod(index, annotatedClass, rolesFieldOrMethod, isPanache);
    }

    public static FieldOrMethod getFieldOrMethod(Index index, ClassInfo annotatedClass,
            AnnotationTarget annotatedFieldOrMethod, boolean isPanache) {
        switch (annotatedFieldOrMethod.kind()) {
            case FIELD:
                // try to find a getter for this field
                FieldInfo field = annotatedFieldOrMethod.asField();
                return new FieldOrMethod(field,
                        findGetter(index, annotatedClass, field, Modifier.isPublic(field.flags()) && isPanache));
            case METHOD:
                // skip the field entirely
                return new FieldOrMethod(null, annotatedFieldOrMethod.asMethod());
            default:
                throw new IllegalArgumentException(
                        "annotatedFieldOrMethod must be a field or method: " + annotatedFieldOrMethod);
        }
    }

    // FIXME: in order to check for the getter type we need to apply type parameters, that's too complex so assume it matches
    private static MethodInfo findGetter(Index index, ClassInfo annotatedClass, FieldInfo field, boolean isPanache) {
        // if it's a panache field, we won't see the getter but it will be there
        String methodName = "get" + JavaBeanUtil.capitalize(field.name());
        if (isPanache) {
            return MethodInfo.create(field.declaringClass(), methodName, new Type[0], field.type(), (short) Modifier.PUBLIC);
        }
        return findGetter(index, annotatedClass, methodName);
    }

    private static MethodInfo findGetter(Index index, ClassInfo annotatedClass, String methodName) {
        MethodInfo method = annotatedClass.method(methodName);
        if (method != null) {
            return method;
        }
        DotName superName = annotatedClass.superName();
        if (superName != null && !superName.equals(QuarkusSecurityJpaProcessor.DOTNAME_OBJECT)) {
            ClassInfo superClass = index.getClassByName(superName);
            if (superClass != null) {
                method = findGetter(index, superClass, methodName);
                if (method != null) {
                    return method;
                }
            }
        }
        for (DotName interfaceName : annotatedClass.interfaceNames()) {
            ClassInfo interf = index.getClassByName(interfaceName);
            if (interf != null) {
                method = findGetter(index, interf, methodName);
                if (method != null) {
                    return method;
                }
            }
        }
        return null;
    }
}
