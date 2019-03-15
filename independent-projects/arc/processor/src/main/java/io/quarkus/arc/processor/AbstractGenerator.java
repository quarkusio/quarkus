/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc.processor;

import io.quarkus.arc.Arc;
import java.lang.reflect.Modifier;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

abstract class AbstractGenerator {

    static final String DEFAULT_PACKAGE = Arc.class.getPackage().getName() + ".generator";

    protected String getBaseName(BeanInfo bean, String beanClassName) {
        String name = Types.getSimpleName(beanClassName);
        return name.substring(0, name.lastIndexOf(BeanGenerator.BEAN_SUFFIX));
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

    protected String getPackageName(BeanInfo bean) {
        String packageName;
        if (bean.isProducerMethod() || bean.isProducerField()) {
            packageName = DotNames.packageName(bean.getDeclaringBean().getProviderType().name());
        } else {
            packageName = DotNames.packageName(bean.getProviderType().name());
        }
        if (packageName.startsWith("java.")) {
            // It is not possible to place a class in a JDK package
            packageName = DEFAULT_PACKAGE;
        }
        return packageName;
    }

}
