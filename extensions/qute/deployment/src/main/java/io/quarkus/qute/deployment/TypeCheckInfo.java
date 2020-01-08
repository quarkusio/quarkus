package io.quarkus.qute.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
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
        List<String> parts;
        Map<String, String> helperHints;
        Type resolvedType;
        ClassInfo rawClass;
        int partsIdx = value.indexOf(RIGHT_BRACKET);
        if (partsIdx + 1 < value.length()) {
            String partsStr = value
                    .substring(partsIdx + 1, value.length());
            parts = new ArrayList<>(Expressions.splitParts(partsStr));
            helperHints = new HashMap<>();
            // [java.util.List<org.acme.Item>]<for>.name
            String firstPart = parts.get(0);
            if (firstPart.equals(helperHint(firstPart))) {
                parts.remove(0);
                helperHints.put(ROOT_HINT, firstPart);
            }
            for (ListIterator<String> iterator = parts.listIterator(); iterator.hasNext();) {
                String part = iterator.next();
                String hint = helperHint(part);
                if (hint != null) {
                    String val = part.substring(0, part.indexOf(LEFT_ANGLE));
                    helperHints.put(val, hint);
                    iterator.set(val);
                }
            }
        } else {
            parts = Collections.emptyList();
            helperHints = Collections.emptyMap();
        }
        String classStr = value.substring(1, value.indexOf("]"));
        if (classStr.equals(Expressions.TYPECHECK_NAMESPACE_PLACEHOLDER)) {
            rawClass = null;
            resolvedType = null;
        } else {
            // "java.util.List<org.acme.Item>" from [java.util.List<org.acme.Item>].name
            DotName rawClassName = rawClassName(classStr);
            rawClass = index.getClassByName(rawClassName);
            if (rawClass == null) {
                throw new TemplateException(
                        "Class [" + rawClassName + "] used in the parameter declaration in template ["
                                + templateIdToPathFun.apply(expression.origin.getTemplateId()) + "] on line "
                                + expression.origin.getLine()
                                + " was not found in the application index. Make sure it is spelled correctly.");
            }
            resolvedType = resolveType(classStr);
        }
        return new TypeCheckInfo(resolvedType, rawClass, parts, helperHints);
    }

    static final String LEFT_ANGLE = "<";
    static final String RIGHT_ANGLE = ">";
    static final String RIGHT_BRACKET = "]";
    static final String ROOT_HINT = "$$root$$";

    final Type resolvedType;
    final ClassInfo rawClass;
    final List<String> parts;
    final Map<String, String> helperHints;

    TypeCheckInfo(Type resolvedType, ClassInfo rawClass, List<String> parts, Map<String, String> helperHints) {
        this.resolvedType = resolvedType;
        this.rawClass = rawClass;
        this.parts = parts;
        this.helperHints = helperHints;
    }

    String getHelperHint(String part) {
        return helperHints.get(part);
    }

    static DotName rawClassName(String value) {
        int angleIdx = value.indexOf(LEFT_ANGLE);
        if (angleIdx != -1) {
            return DotName.createSimple(value.substring(0, angleIdx));
        } else {
            return DotName.createSimple(value);
        }
    }

    static String helperHint(String part) {
        int angleIdx = part.indexOf(LEFT_ANGLE);
        if (angleIdx == -1) {
            return null;
        }
        return part.substring(angleIdx, part.length());
    }

    static Type resolveType(String value) {
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

}
