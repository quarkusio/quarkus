package io.quarkus.security.jpa.common.deployment;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.gizmo2.Jandex2Gizmo;

import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.creator.BlockCreator;

public class JpaSecurityDefinition {

    public boolean haveRolesAnnotation(DotName... annotations) {
        for (DotName annotation : annotations) {
            if (roles.annotation(annotation) != null) {
                return true;
            }
        }
        return false;
    }

    public static class FieldOrMethod {
        public final FieldInfo field;
        public final MethodInfo getter;
        private final Type type;

        public FieldOrMethod(FieldInfo field, MethodInfo getter) {
            this(field, getter, field != null ? field.type() : getter.returnType());
        }

        FieldOrMethod(FieldInfo field, MethodInfo getter, Type type) {
            this.field = field;
            this.getter = getter;
            this.type = type;
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

        public Expr readValue(BlockCreator bc, Expr userVar) {
            // favour the getter
            if (getter != null) {
                return bc.invokeVirtual(Jandex2Gizmo.methodDescOf(getter), userVar);
            }
            return bc.get(userVar.field(Jandex2Gizmo.fieldDescOf(field)));
        }

        public Type type() {
            return type;
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

        if (annotatedFieldOrMethod == null) {
            return null;
        }

        switch (annotatedFieldOrMethod.kind()) {
            case FIELD:
                // try to find a getter for this field
                FieldInfo field = annotatedFieldOrMethod.asField();
                return new FieldOrMethod(field,
                        findGetter(index, annotatedClass, field, Modifier.isPublic(field.flags()) && isPanache),
                        resolveType(index, annotatedClass, field.declaringClass(), field.type()));
            case METHOD:
                // skip the field entirely
                MethodInfo method = annotatedFieldOrMethod.asMethod();
                return new FieldOrMethod(null, method,
                        resolveType(index, annotatedClass, method.declaringClass(), method.returnType()));
            default:
                throw new IllegalArgumentException(
                        "annotatedFieldOrMethod must be a field or method: " + annotatedFieldOrMethod);
        }
    }

    private static Type resolveType(Index index, ClassInfo annotatedClass, ClassInfo declaringClass, Type type) {
        List<TypeVariable> typeParameters = declaringClass.typeParameters();
        if (typeParameters.isEmpty() || declaringClass.name().equals(annotatedClass.name())) {
            return type;
        }
        List<Type> arguments = JandexUtil.resolveTypeParameters(annotatedClass.name(), declaringClass.name(), index);
        if (arguments.size() != typeParameters.size()) {
            return type;
        }
        Map<String, Type> mapping = new HashMap<>();
        for (int i = 0; i < typeParameters.size(); i++) {
            mapping.put(typeParameters.get(i).identifier(), arguments.get(i));
        }
        return substitute(type, mapping);
    }

    private static Type substitute(Type type, Map<String, Type> mapping) {
        return switch (type.kind()) {
            case TYPE_VARIABLE -> mapping.getOrDefault(type.asTypeVariable().identifier(), type);
            case PARAMETERIZED_TYPE -> {
                ParameterizedType parameterized = type.asParameterizedType();
                Type[] arguments = parameterized.arguments().stream()
                        .map(argument -> substitute(argument, mapping))
                        .toArray(Type[]::new);
                yield ParameterizedType.create(parameterized.name(), arguments, parameterized.owner());
            }
            default -> type;
        };
    }

    public AnnotationValue passwordType() {
        return password.annotation(QuarkusSecurityJpaCommonProcessor.DOTNAME_PASSWORD).value();
    }

    public AnnotationValue customPasswordProvider() {
        return password.annotation(QuarkusSecurityJpaCommonProcessor.DOTNAME_PASSWORD).value("provider");
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
        if (superName != null && !superName.equals(DotNames.OBJECT)) {
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
