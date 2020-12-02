package io.quarkus.qute.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.qute.Expression;
import io.quarkus.qute.Expressions;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateNode.Origin;

final class TypeInfos {

    static List<Info> create(Expression expression, IndexView index, Function<String, String> templateIdToPathFun) {
        if (expression.isLiteral()) {
            Expression.Part literalPart = expression.getParts().get(0);
            return Collections
                    .singletonList(create(literalPart.getTypeInfo(), literalPart, index,
                            templateIdToPathFun, expression.getOrigin()));
        }
        List<Info> infos = new ArrayList<>();
        boolean splitParts = true;
        for (Expression.Part part : expression.getParts()) {
            if (splitParts) {
                List<String> infoParts = Expressions.splitTypeInfoParts(part.getTypeInfo());
                for (String infoPart : infoParts) {
                    infos.add(create(infoPart, part, index, templateIdToPathFun, expression.getOrigin()));
                }
            } else {
                infos.add(create(part.getTypeInfo(), part, index, templateIdToPathFun, expression.getOrigin()));
            }
            splitParts = false;
        }
        return infos;
    }

    static Info create(String typeInfo, Expression.Part part, IndexView index, Function<String, String> templateIdToPathFun,
            Origin expressionOrigin) {
        if (typeInfo.startsWith(TYPE_INFO_SEPARATOR)) {
            int endIdx = typeInfo.substring(1, typeInfo.length()).indexOf(Expressions.TYPE_INFO_SEPARATOR);
            if (endIdx < 1) {
                throw new IllegalArgumentException("Invalid type info: " + typeInfo);
            }
            String classStr = typeInfo.substring(1, endIdx + 1);
            if (classStr.equals(Expressions.TYPECHECK_NAMESPACE_PLACEHOLDER)) {
                return new Info(typeInfo, part);
            } else {
                DotName rawClassName = rawClassName(classStr);
                ClassInfo rawClass = index.getClassByName(rawClassName);
                if (rawClass == null) {
                    throw new TemplateException(
                            "Class [" + rawClassName + "] used in the parameter declaration in template ["
                                    + templateIdToPathFun.apply(expressionOrigin.getTemplateGeneratedId()) + "] on line "
                                    + expressionOrigin.getLine()
                                    + " was not found in the application index. Make sure it is spelled correctly.");
                }
                Type resolvedType = resolveType(classStr);
                return new TypeInfo(typeInfo, part, helperHint(typeInfo.substring(endIdx, typeInfo.length())), resolvedType,
                        rawClass);
            }
        } else {
            String hint = helperHint(typeInfo);
            if (hint != null) {
                typeInfo = typeInfo.substring(0, typeInfo.indexOf(LEFT_ANGLE));
            }
            if (part.isVirtualMethod() || Expressions.isVirtualMethod(typeInfo)) {
                return new VirtualMethodInfo(typeInfo, part.asVirtualMethod());
            }
            return new PropertyInfo(typeInfo, part, hint);
        }
    }

    static final String LEFT_ANGLE = "<";
    static final String RIGHT_ANGLE = ">";
    static final String TYPE_INFO_SEPARATOR = "" + Expressions.TYPE_INFO_SEPARATOR;

    private static DotName rawClassName(String value) {
        int angleIdx = value.indexOf(LEFT_ANGLE);
        if (angleIdx != -1) {
            return DotName.createSimple(value.substring(0, angleIdx));
        } else {
            return DotName.createSimple(value);
        }
    }

    private static String helperHint(String part) {
        int angleIdx = part.indexOf(LEFT_ANGLE);
        if (angleIdx == -1) {
            return null;
        }
        return part.substring(angleIdx, part.length());
    }

    private static Type resolveType(String value) {
        int angleIdx = value.indexOf(LEFT_ANGLE);
        if (angleIdx == -1) {
            return Type.create(DotName.createSimple(value), Kind.CLASS);
        } else {
            String name = value.substring(0, angleIdx);
            DotName rawName = DotName.createSimple(name);
            String[] parts = value.substring(angleIdx + 1, value.length() - 1).split(",");
            Type[] arguments = new Type[parts.length];
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = resolveType(parts[i]);
            }
            return ParameterizedType.create(rawName, arguments, null);
        }
    }

    private TypeInfos() {
    }

    static class Info {

        final String value;
        final Expression.Part part;

        public Info(String value, Expression.Part part) {
            this.value = value;
            this.part = part;
        }

        boolean isVirtualMethod() {
            return false;
        }

        boolean isProperty() {
            return false;
        }

        boolean isTypeInfo() {
            return false;
        }

        VirtualMethodInfo asVirtualMethod() {
            throw new IllegalArgumentException("Not a virtual method");
        }

        PropertyInfo asProperty() {
            throw new IllegalArgumentException("Not a property");
        }

        TypeInfo asTypeInfo() {
            throw new IllegalArgumentException("Not a type info: " + getClass().getName() + ":" + toString());
        }

        @Override
        public String toString() {
            return value;
        }

    }

    static abstract class HintInfo extends Info {

        final String hint;

        public HintInfo(String value, Expression.Part part, String hint) {
            super(value, part);
            this.hint = hint;
        }

    }

    static class TypeInfo extends HintInfo {

        final Type resolvedType;
        final ClassInfo rawClass;

        public TypeInfo(String value, Expression.Part part, String hint, Type resolvedType, ClassInfo rawClass) {
            super(value, part, hint);
            this.resolvedType = resolvedType;
            this.rawClass = rawClass;
        }

        @Override
        boolean isTypeInfo() {
            return true;
        }

        @Override
        TypeInfo asTypeInfo() {
            return this;
        }

    }

    static class PropertyInfo extends HintInfo {

        final String name;

        public PropertyInfo(String name, Expression.Part part, String hint) {
            super(name, part, hint);
            this.name = name;
        }

        @Override
        public boolean isProperty() {
            return true;
        }

        @Override
        public PropertyInfo asProperty() {
            return this;
        }

    }

    static class VirtualMethodInfo extends Info {

        final String name;

        public VirtualMethodInfo(String value, Expression.VirtualMethodPart part) {
            super(value, part);
            this.name = part.getName();
        }

        @Override
        public boolean isVirtualMethod() {
            return true;
        }

        @Override
        public VirtualMethodInfo asVirtualMethod() {
            return this;
        }

    }

}
