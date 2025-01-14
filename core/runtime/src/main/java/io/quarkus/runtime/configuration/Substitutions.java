package io.quarkus.runtime.configuration;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.graalvm.nativeimage.MissingReflectionRegistrationError;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;

import io.smallrye.common.constraint.Assert;
import io.smallrye.config.AbstractLocationConfigSourceLoader;
import io.smallrye.config.ConfigMappingInterface;
import io.smallrye.config.ConfigMappingLoader;
import io.smallrye.config.ConfigMappingMetadata;
import io.smallrye.config._private.ConfigMessages;

/**
 */
final class Substitutions {
    @TargetClass(ConfigProviderResolver.class)
    static final class Target_ConfigurationProviderResolver {

        @Alias
        @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
        private static volatile ConfigProviderResolver instance;
    }

    @TargetClass(ConfigMappingLoader.class)
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

    @TargetClass(ConfigMappingInterface.class)
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

    @TargetClass(value = ConfigMappingLoader.class, innerClass = "ConfigMappingClass")
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

    @TargetClass(value = AbstractLocationConfigSourceLoader.class)
    static final class Target_AbstractLocationConfigSourceLoader {
        @Alias
        protected native List<ConfigSource> tryClassPath(final URI uri, final int ordinal, final ClassLoader classLoader);

        @Alias
        protected native List<ConfigSource> tryFileSystem(final URI uri, final int ordinal);

        @Alias
        protected native List<ConfigSource> tryJar(final URI uri, final int ordinal);

        @Alias
        protected native List<ConfigSource> tryHttpResource(final URI uri, final int ordinal);

        @Alias
        private static Converter<URI> URI_CONVERTER = null;

        @Substitute
        protected List<ConfigSource> loadConfigSources(final String[] locations, final int ordinal,
                final ClassLoader classLoader) {
            if (locations == null || locations.length == 0) {
                return Collections.emptyList();
            }

            final List<ConfigSource> configSources = new ArrayList<>();
            for (String location : locations) {
                final URI uri = URI_CONVERTER.convert(location);
                if (uri.getScheme() == null) {
                    configSources.addAll(tryFileSystem(uri, ordinal));
                    try {
                        configSources.addAll(tryClassPath(uri, ordinal, classLoader));
                    } catch (MissingReflectionRegistrationError e) {
                        // ignore
                    }
                } else if (uri.getScheme().equals("file")) {
                    configSources.addAll(tryFileSystem(uri, ordinal));
                } else if (uri.getScheme().equals("jar")) {
                    configSources.addAll(tryJar(uri, ordinal));
                } else if (uri.getScheme().startsWith("http")) {
                    configSources.addAll(tryHttpResource(uri, ordinal));
                } else {
                    throw ConfigMessages.msg.schemeNotSupported(uri.getScheme());
                }
            }
            return configSources;
        }
    }
}
