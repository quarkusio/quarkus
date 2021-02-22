package io.quarkus.arc.processor;

import io.quarkus.arc.Arc;
import java.lang.reflect.Modifier;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type.Kind;

abstract class AbstractGenerator {

    static final String DEFAULT_PACKAGE = Arc.class.getPackage().getName() + ".generator";
    static final String UNDERSCORE = "_";
    static final String SYNTHETIC_SUFFIX = "Synthetic";

    protected final boolean generateSources;

    public AbstractGenerator(boolean generateSources) {
        this.generateSources = generateSources;
    }

    /**
     * Create a generated bean name from a bean package. When bean is located
     * in a default package (i.e. a classpath root), the target package name
     * is empty string. This need to be taken into account when creating
     * generated bean name because it is later used to build class file path
     * and we do not want it to start from a slash because it will point root
     * directory instead of a relative one. This method will address this
     * problem.<br>
     * <br>
     * Example generated bean names (without quotes):
     * <ol>
     * <li>a <i>"io/quarcus/foo/FooService_Bean"</i>, when in io.quarcus.foo package,</li>
     * <li>a <i>"BarService_Bean"</i>, when in default package.</li>
     * </ol>
     *
     * @param baseName a bean name (class name)
     * @param targetPackage a package where bean is located
     * @return Generated name
     */
    static String generatedNameFromTarget(String targetPackage, String baseName, String suffix) {
        if (targetPackage == null || targetPackage.isEmpty()) {
            return baseName + suffix;
        } else {
            return targetPackage.replace('.', '/') + "/" + baseName + suffix;
        }
    }

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
        DotName providerTypeName;
        if (bean.isProducerMethod() || bean.isProducerField()) {
            providerTypeName = bean.getDeclaringBean().getProviderType().name();
        } else {
            if (bean.getProviderType().kind() == Kind.ARRAY || bean.getProviderType().kind() == Kind.PRIMITIVE) {
                providerTypeName = bean.getImplClazz().name();
            } else {
                providerTypeName = bean.getProviderType().name();
            }
        }
        String packageName = DotNames.packageName(providerTypeName);
        if (packageName.startsWith("java.")) {
            // It is not possible to place a class in a JDK package
            packageName = DEFAULT_PACKAGE;
        }
        return packageName;
    }

}
