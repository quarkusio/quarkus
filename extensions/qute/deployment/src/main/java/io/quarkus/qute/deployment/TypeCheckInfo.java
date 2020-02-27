package io.quarkus.qute.deployment;

import java.util.ArrayList;
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

class TypeCheckInfo {

    static TypeCheckInfo create(Expression expression, IndexView index, Function<String, String> templateIdToPathFun) {
        String value = expression.typeCheckInfo;
        List<Part> parts = new ArrayList<>();
        // Complex example: |java.util.List<org.acme.Item>|<for-element>.bar.call(|org.acme.Foo|).items<for-element>
        // Expected results:
        // * root type info based on |java.util.List<org.acme.Item>|
        // * root hint is <for-element>
        // * 3 parts:
        // ** property "bar"
        // ** virtual method "call" with type info param based on |org.acme.Foo|
        // ** property "items" with hint <for-element>
        for (String partStr : Expressions.splitTypeCheckParts(value)) {
            parts.add(createPart(partStr, index, templateIdToPathFun, expression));
        }
        return new TypeCheckInfo(parts);
    }

    private static Part createPart(String value, IndexView index, Function<String, String> templateIdToPathFun,
            Expression expression) {
        if (value.startsWith(TYPE_INFO_SEPARATOR)) {
            int endIdx = value.substring(1, value.length()).indexOf(Expressions.TYPE_INFO_SEPARATOR);
            if (endIdx < 1) {
                throw new IllegalArgumentException("Invalid type info: " + value);
            }
            String classStr = value.substring(1, endIdx + 1);
            if (classStr.equals(Expressions.TYPECHECK_NAMESPACE_PLACEHOLDER)) {
                return new Part(value);
            } else {
                DotName rawClassName = rawClassName(classStr);
                ClassInfo rawClass = index.getClassByName(rawClassName);
                if (rawClass == null) {
                    throw new TemplateException(
                            "Class [" + rawClassName + "] used in the parameter declaration in template ["
                                    + templateIdToPathFun.apply(expression.origin.getTemplateGeneratedId()) + "] on line "
                                    + expression.origin.getLine()
                                    + " was not found in the application index. Make sure it is spelled correctly.");
                }
                Type resolvedType = resolveType(classStr);
                return new TypeInfoPart(value, helperHint(value.substring(endIdx, value.length())), resolvedType, rawClass);
            }
        } else if (Expressions.isVirtualMethod(value)) {
            List<String> params = Expressions.parseVirtualMethodParams(value);
            List<Part> paramParts = new ArrayList<>(params.size());
            for (String param : params) {
                paramParts.add(createPart(param, index, templateIdToPathFun, expression));
            }
            return new VirtualMethodPart(value, Expressions.parseVirtualMethodName(value), paramParts);
        } else {
            String hint = helperHint(value);
            if (hint != null) {
                value = value.substring(0, value.indexOf(LEFT_ANGLE));
            }
            return new PropertyPart(value, hint);
        }
    }

    static final String LEFT_ANGLE = "<";
    static final String RIGHT_ANGLE = ">";
    static final String ROOT_HINT = "$$root$$";
    static final String TYPE_INFO_SEPARATOR = "" + Expressions.TYPE_INFO_SEPARATOR;

    final List<Part> parts;

    TypeCheckInfo(List<Part> parts) {
        this.parts = parts;
    }

    TypeInfoPart getRoot() {
        return parts.get(0).asTypeInfo();
    }

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

    static class Part {

        final String value;

        public Part(String value) {
            this.value = value;
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

        VirtualMethodPart asVirtualMethod() {
            throw new IllegalArgumentException("Not a virtual method");
        }

        PropertyPart asProperty() {
            throw new IllegalArgumentException("Not a property");
        }

        TypeInfoPart asTypeInfo() {
            throw new IllegalArgumentException("Not a type info");
        }

        @Override
        public String toString() {
            return value;
        }

    }

    static abstract class HintPart extends Part {

        final String hint;

        public HintPart(String value, String hint) {
            super(value);
            this.hint = hint;
        }

    }

    static class TypeInfoPart extends HintPart {

        final Type resolvedType;
        final ClassInfo rawClass;

        public TypeInfoPart(String value, String hint, Type resolvedType, ClassInfo rawClass) {
            super(value, hint);
            this.resolvedType = resolvedType;
            this.rawClass = rawClass;
        }

        @Override
        boolean isTypeInfo() {
            return true;
        }

        @Override
        TypeInfoPart asTypeInfo() {
            return this;
        }

    }

    static class PropertyPart extends HintPart {

        final String name;

        public PropertyPart(String name, String hint) {
            super(name, hint);
            this.name = name;
        }

        @Override
        public boolean isProperty() {
            return true;
        }

        @Override
        public PropertyPart asProperty() {
            return this;
        }

    }

    static class VirtualMethodPart extends Part {

        final String name;
        final List<Part> parameters;

        public VirtualMethodPart(String value, String name, List<Part> parameters) {
            super(value);
            this.name = name;
            this.parameters = parameters;
        }

        @Override
        public boolean isVirtualMethod() {
            return true;
        }

        @Override
        public VirtualMethodPart asVirtualMethod() {
            return this;
        }

    }

}
