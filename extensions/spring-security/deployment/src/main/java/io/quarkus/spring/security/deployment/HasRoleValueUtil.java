package io.quarkus.spring.security.deployment;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.spring.security.deployment.roles.FromBeanHasRoleValueProducer;
import io.quarkus.spring.security.deployment.roles.HasRoleValueProducer;
import io.quarkus.spring.security.deployment.roles.StaticHasRoleValueProducer;

final class HasRoleValueUtil {

    private static final String BEAN_FIELD_REGEX = "@(\\w+)\\.(\\w+)";
    private static final Pattern BEAN_FIELD_PATTERN = Pattern.compile(BEAN_FIELD_REGEX);

    private HasRoleValueUtil() {
    }

    static HasRoleValueProducer getHasRoleValueProducer(String hasRoleValue, MethodInfo methodInfo, IndexView index,
            Map<String, DotName> springBeansNameToDotName,
            Map<String, ClassInfo> springBeansNameToClassInfo, Set<String> beansReferencedInPreAuthorized) {
        if (hasRoleValue.startsWith("'") && hasRoleValue.endsWith("'")) {
            return new StaticHasRoleValueProducer(hasRoleValue.replace("'", ""));
        } else if (hasRoleValue.startsWith("@")) {
            Matcher beanFieldMatcher = BEAN_FIELD_PATTERN.matcher(hasRoleValue);
            if (!beanFieldMatcher.find()) {
                throw SpringSecurityProcessorUtil.createGenericMalformedException(methodInfo, hasRoleValue);
            }

            String beanName = beanFieldMatcher.group(1);
            ClassInfo beanClassInfo = SpringSecurityProcessorUtil.getClassInfoFromBeanName(beanName, index,
                    springBeansNameToDotName, springBeansNameToClassInfo, hasRoleValue, methodInfo);

            String fieldName = beanFieldMatcher.group(2);
            FieldInfo fieldInfo = beanClassInfo.field(fieldName);
            if ((fieldInfo == null) || !Modifier.isPublic(fieldInfo.flags())
                    || !DotNames.STRING.equals(fieldInfo.type().name())) {
                throw new IllegalArgumentException("Bean named '" + beanName + "' found in expression '" + hasRoleValue
                        + "' in the @PreAuthorize annotation on method " + methodInfo.name() + " of class "
                        + methodInfo.declaringClass() + " does not have a public field named '" + fieldName
                        + "' of type String");
            }

            beansReferencedInPreAuthorized.add(fieldInfo.declaringClass().name().toString());

            return new FromBeanHasRoleValueProducer(beanName, fieldInfo);
        } else {
            throw SpringSecurityProcessorUtil.createGenericMalformedException(methodInfo, hasRoleValue);
        }
    }

}
