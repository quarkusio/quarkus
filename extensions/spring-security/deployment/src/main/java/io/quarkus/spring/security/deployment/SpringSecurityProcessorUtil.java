package io.quarkus.spring.security.deployment;

import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

final class SpringSecurityProcessorUtil {

    static final String BASIC_BEAN_METHOD_INVOCATION_REGEX = "@(\\w+)\\.(\\w+)\\(.*\\)";
    static final Pattern BASIC_BEAN_METHOD_INVOCATION_PATTERN = Pattern.compile(BASIC_BEAN_METHOD_INVOCATION_REGEX);

    private SpringSecurityProcessorUtil() {
    }

    static IllegalArgumentException createGenericMalformedException(MethodInfo methodInfo, String expression) {
        return new IllegalArgumentException(
                "Expression: '" + expression + "' in the @PreAuthorize annotation on method '" + methodInfo.name()
                        + "' of class '" + methodInfo.declaringClass() + "' is malformed");
    }

    static ClassInfo getClassInfoFromBeanName(String beanName, IndexView index, Map<String, DotName> springBeansNameToDotName,
            Map<String, ClassInfo> springBeansNameToClassInfo,
            String expression, MethodInfo methodInfo) {
        ClassInfo beanClassInfo = springBeansNameToClassInfo.get(beanName);
        if (beanClassInfo == null) {
            DotName beanClassDotName = springBeansNameToDotName.get(beanName);
            if (beanClassDotName == null) {
                throw new IllegalArgumentException("Could not find bean named '" + beanName
                        + "' found in expression" + expression + "' in the @PreAuthorize annotation on method "
                        + methodInfo.name() + " of class " + methodInfo.declaringClass()
                        + " in the set of the application beans");
            }
            beanClassInfo = index.getClassByName(beanClassDotName);
            if (beanClassInfo == null) {
                throw new IllegalStateException("Unable to locate class " + beanClassDotName + " in the index");
            }
            springBeansNameToClassInfo.put(beanName, beanClassInfo);
        }
        return beanClassInfo;
    }

    static int getParameterIndex(MethodInfo methodInfo, String parameterName, String expression) {
        int parametersCount = methodInfo.parameters().size();
        int matchingParameterIndex = -1;
        for (int i = 0; i < parametersCount; i++) {
            if (parameterName.equals(methodInfo.parameterName(i))) {
                matchingParameterIndex = i;
                break;
            }
        }

        if (matchingParameterIndex == -1) {
            throw new IllegalArgumentException(
                    "Expression: '" + expression + "' in the @PreAuthorize annotation on method '" + methodInfo.name()
                            + "' of class '" + methodInfo.declaringClass() + "' references parameter " + parameterName
                            + " that the method does not declare");
        }
        return matchingParameterIndex;
    }
}
