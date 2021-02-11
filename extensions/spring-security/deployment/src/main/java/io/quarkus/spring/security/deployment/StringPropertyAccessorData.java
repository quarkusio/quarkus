package io.quarkus.spring.security.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

class StringPropertyAccessorData {

    private final ClassInfo matchingParameterClassInfo;
    private final FieldInfo matchingParameterFieldInfo;

    private StringPropertyAccessorData(ClassInfo matchingParameterClassInfo,
            FieldInfo matchingParameterFieldInfo) {
        this.matchingParameterClassInfo = matchingParameterClassInfo;
        this.matchingParameterFieldInfo = matchingParameterFieldInfo;
    }

    /**
     * Called with data parsed from a Spring expression like #person.name that is places inside a Spring security annotation on
     * a method
     */
    static StringPropertyAccessorData from(MethodInfo methodInfo, int matchingParameterIndex, String propertyName,
            IndexView index, String expression) {
        Type matchingParameterType = methodInfo.parameters().get(matchingParameterIndex);
        ClassInfo matchingParameterClassInfo = index.getClassByName(matchingParameterType.name());
        if (matchingParameterClassInfo == null) {
            throw new IllegalArgumentException(
                    "Expression: '" + expression + "' in the @PreAuthorize annotation on method '" + methodInfo.name()
                            + "' of class '" + methodInfo.declaringClass() + "' references class "
                            + matchingParameterType.name() + " which could not be in Jandex");
        }
        FieldInfo matchingParameterFieldInfo = matchingParameterClassInfo.field(propertyName);
        if (matchingParameterFieldInfo == null) {
            throw new IllegalArgumentException(
                    "Expression: '" + expression + "' in the @PreAuthorize annotation on method '" + methodInfo.name()
                            + "' of class '" + methodInfo.declaringClass() + "' references unknown property '"
                            + propertyName + "' of class " + matchingParameterClassInfo);
        }
        if (!DotNames.STRING.equals(matchingParameterFieldInfo.type().name())) {
            throw new IllegalArgumentException(
                    "Expression: '" + expression + "' in the @PreAuthorize annotation on method '" + methodInfo.name()
                            + "' of class '" + methodInfo.declaringClass() + "' references property '"
                            + propertyName + "' which is not a string");
        }

        return new StringPropertyAccessorData(matchingParameterClassInfo, matchingParameterFieldInfo);
    }

    public ClassInfo getMatchingParameterClassInfo() {
        return matchingParameterClassInfo;
    }

    public FieldInfo getMatchingParameterFieldInfo() {
        return matchingParameterFieldInfo;
    }
}
