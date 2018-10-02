package org.jboss.protean.arc.processor;

import java.lang.reflect.Modifier;

import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

abstract class AbstractGenerator {

    protected String providerName(String name) {
        // TODO we can do better
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    protected String getBaseName(BeanInfo bean, String beanClassName) {
        String name = Types.getSimpleName(beanClassName);
        return name.substring(0, name.indexOf(BeanGenerator.BEAN_SUFFIX));
    }

    protected boolean isReflectionFallbackNeeded(MethodInfo method, String targetPackage) {
        // Reflection fallback is needed for private methods and non-public methods declared on superclasses located in a different package
        if (Modifier.isPrivate(method.flags())) {
            return true;
        }
        if (Modifier.isProtected(method.flags()) || isPackagePrivate(method.flags())) {
            return !DotNames.packageName(method.declaringClass().name()).equals(targetPackage);
        }
        return false;
    }

    protected boolean isReflectionFallbackNeeded(FieldInfo field, String targetPackage) {
        // Reflection fallback is needed for private fields and non-public fields declared on superclasses located in a different package
        if (Modifier.isPrivate(field.flags())) {
            return true;
        }
        if (Modifier.isProtected(field.flags()) || isPackagePrivate(field.flags())) {
            return !DotNames.packageName(field.declaringClass().name()).equals(targetPackage);
        }
        return false;
    }

    protected boolean isPackagePrivate(int mod) {
        return !(Modifier.isPrivate(mod) || Modifier.isProtected(mod) || Modifier.isPublic(mod));
    }

}
