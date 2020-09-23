package io.quarkus.runtime.configuration;

import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;
import com.oracle.svm.core.threadlocal.FastThreadLocalFactory;
import com.oracle.svm.core.threadlocal.FastThreadLocalInt;

import io.smallrye.common.constraint.Assert;
import io.smallrye.config.ConfigMappingMetadata;
import io.smallrye.config.Expressions;

/**
 */
final class Substitutions {
    // 0 = expand so that the default value is to expand
    static final FastThreadLocalInt notExpanding = FastThreadLocalFactory.createInt();

    @TargetClass(ConfigProviderResolver.class)
    static final class Target_ConfigurationProviderResolver {

        @Alias
        @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
        private static volatile ConfigProviderResolver instance;
    }

    @TargetClass(Expressions.class)
    static final class Target_Expressions {
        @Delete
        private static ThreadLocal<Boolean> ENABLE;

        @Substitute
        private static boolean isEnabled() {
            return notExpanding.get() == 0;
        }

        @Substitute
        public static <T> T withoutExpansion(Supplier<T> supplier) {
            if (isEnabled()) {
                notExpanding.set(1);
                try {
                    return supplier.get();
                } finally {
                    notExpanding.set(0);
                }
            } else {
                return supplier.get();
            }
        }
    }

    @TargetClass(className = "io.smallrye.config.ConfigMappingLoader")
    static final class Target_ConfigMappingLoader {
        @Substitute
        static Class<?> loadClass(final Class<?> parent, final ConfigMappingMetadata configMappingMetadata) {
            try {
                return parent.getClassLoader().loadClass(configMappingMetadata.getClassName());
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        @Substitute
        private static Class<?> defineClass(final Class<?> parent, final String className, final byte[] classBytes) {
            return null;
        }
    }

    @TargetClass(className = "io.smallrye.config.ConfigMappingInterface")
    static final class Target_ConfigMappingInterface {
        @Alias
        static ClassValue<Target_ConfigMappingInterface> cv = null;

        // ClassValue is substituted by a regular ConcurrentHashMap - java.lang.ClassValue.get(JavaLangSubstitutions.java:514)
        @Substitute
        public static Target_ConfigMappingInterface getConfigurationInterface(Class<?> interfaceType) {
            Assert.checkNotNullParam("interfaceType", interfaceType);
            try {
                return cv.get(interfaceType);
            } catch (NullPointerException e) {
                return null;
            }
        }

        // This should not be called, but we substitute it anyway to make sure we remove any references to ASM classes.
        @Substitute
        public byte[] getClassBytes() {
            return null;
        }
    }

    @TargetClass(className = "io.smallrye.config.ConfigMappingClass")
    static final class Target_ConfigMappingClass {
        @Alias
        static ClassValue<Target_ConfigMappingClass> cv = null;

        // ClassValue is substituted by a regular ConcurrentHashMap - java.lang.ClassValue.get(JavaLangSubstitutions.java:514)
        @Substitute
        public static Target_ConfigMappingClass getConfigurationClass(Class<?> classType) {
            Assert.checkNotNullParam("classType", classType);
            try {
                return cv.get(classType);
            } catch (NullPointerException e) {
                return null;
            }
        }

        @Alias
        private Class<?> classType;
        @Alias
        private String interfaceName;

        @Substitute
        @TargetElement(name = TargetElement.CONSTRUCTOR_NAME)
        public Target_ConfigMappingClass(final Class<?> classType) {
            this.classType = classType;
            this.interfaceName = classType.getPackage().getName() + "." + classType.getSimpleName()
                    + classType.getName().hashCode() + "I";
        }

        // This should not be called, but we substitute it anyway to make sure we remove any references to ASM classes.
        @Substitute
        public byte[] getClassBytes() {
            return null;
        }
    }
}
