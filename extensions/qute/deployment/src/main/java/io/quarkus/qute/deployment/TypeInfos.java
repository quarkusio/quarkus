package io.quarkus.qute.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.qute.Expression;
import io.quarkus.qute.Expressions;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateNode.Origin;

final class TypeInfos {

    private static final String ARRAY_DIM = "[]";

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
                // TODO make the parsing logic more robust 
                ClassInfo rawClass;
                Type resolvedType;
                int idx = classStr.indexOf(ARRAY_DIM);
                if (idx > 0) {
                    // int[], java.lang.String[][], etc.
                    String componentTypeStr = classStr.substring(0, idx);
                    Type componentType = decodePrimitive(componentTypeStr);
                    if (componentType == null) {
                        componentType = resolveType(componentTypeStr);
                    }
                    String[] dimensions = classStr.substring(idx, classStr.length()).split("\\]");
                    rawClass = null;
                    resolvedType = ArrayType.create(componentType, dimensions.length);
                } else {
                    rawClass = getClassInfo(classStr, index, templateIdToPathFun, expressionOrigin);
                    resolvedType = resolveType(classStr);
                }
                return new TypeInfo(typeInfo, part, helperHint(typeInfo.substring(endIdx, typeInfo.length())), resolvedType,
                        rawClass);
            }
        } else {
            String hint = helperHint(typeInfo);
            if (hint != null) {
                typeInfo = typeInfo.substring(0, typeInfo.indexOf(LEFT_ANGLE));
            }
            if (part.isVirtualMethod() || Expressions.isVirtualMethod(typeInfo)) {
                return new VirtualMethodInfo(typeInfo, part.asVirtualMethod(), hint);
            }
            return new PropertyInfo(part.getName(), part, hint);
        }
    }

    private static ClassInfo getClassInfo(String val, IndexView index, Function<String, String> templateIdToPathFun,
            Origin expressionOrigin) {
        DotName rawClassName = rawClassName(val);
        ClassInfo clazz = index.getClassByName(rawClassName);
        if (clazz == null) {
            throw new TemplateException(
                    "Class [" + rawClassName + "] used in the parameter declaration in template ["
                            + templateIdToPathFun.apply(expressionOrigin.getTemplateGeneratedId()) + "] on line "
                            + expressionOrigin.getLine()
                            + " was not found in the application index. Make sure it is spelled correctly.");
        }
        return clazz;
    }

    private static PrimitiveType decodePrimitive(String val) {
        switch (val) {
            case "byte":
                return PrimitiveType.BYTE;
            case "char":
                return PrimitiveType.CHAR;
            case "double":
                return PrimitiveType.DOUBLE;
            case "float":
                return PrimitiveType.FLOAT;
            case "int":
                return PrimitiveType.INT;
            case "long":
                return PrimitiveType.LONG;
            case "short":
                return PrimitiveType.SHORT;
            case "boolean":
                return PrimitiveType.BOOLEAN;
            default:
                return null;
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
                arguments[i] = resolveType(parts[i].trim());
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

        boolean hasHints() {
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

        HintInfo asHintInfo() {
            throw new IllegalArgumentException("Not a hint info");
        }

        @Override
        public String toString() {
            return value;
        }

    }

    static abstract class HintInfo extends Info {

        static final Pattern HINT_PATTERN = Pattern.compile("\\<[a-zA-Z_0-9#-]+\\>");

        // <loop#1>, <set#10><loop-element>, etc.
        final List<String> hints;

        HintInfo(String value, Expression.Part part, String hintStr) {
            super(value, part);
            if (hintStr != null) {
                List<String> found = new ArrayList<>();
                Matcher m = HINT_PATTERN.matcher(hintStr);
                while (m.find()) {
                    found.add(m.group());
                }
                this.hints = found;
            } else {
                this.hints = Collections.emptyList();
            }
        }

        boolean hasHints() {
            return !hints.isEmpty();
        }

        @Override
        HintInfo asHintInfo() {
            return this;
        }

    }

    static class TypeInfo extends HintInfo {

        final Type resolvedType;
        final ClassInfo rawClass;

        TypeInfo(String value, Expression.Part part, String hint, Type resolvedType, ClassInfo rawClass) {
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

        PropertyInfo(String name, Expression.Part part, String hint) {
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

    static class VirtualMethodInfo extends HintInfo {

        final String name;

        VirtualMethodInfo(String value, Expression.VirtualMethodPart part, String hint) {
            super(value, part, hint);
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
