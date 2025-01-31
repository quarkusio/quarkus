package io.quarkus.runtime.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;

import io.smallrye.common.constraint.Assert;
import io.smallrye.config.ConfigMappingInterface;
import io.smallrye.config.ConfigMappingLoader;
import io.smallrye.config.ConfigMappingMetadata;

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

    /**
     * The GraalVM provides a lazy implementation to access system properties that are expensive to calculate. Still, it
     * ends up calculating all the properties anyway when {@link System#getProperties()} is called, which is a common
     * call. Used, for instance, to get the list of names in Quarkus configuration, but also in
     * GetPropertyAction#privilegedGetProperties() is used in many JVM APIs, for instance, when determining the default
     * timezone. Such initialization may cost a few milliseconds of the native image startup time (measured between 5-6,
     * depending on the system).
     * <p>
     * This Substitution provides a delegate to the GraalVM lazy implementation, expanding the lazy check to each
     * individual method of {@link Properties}.
     */
    @TargetClass(className = "com.oracle.svm.core.jdk.SystemPropertiesSupport")
    static final class Target_SystemPropertiesSupport {
        @Alias
        private Properties properties;

        @Alias
        private void ensureFullyInitialized() {
        }

        @Alias
        private void initializeLazyValue(String key) {
        }

        @Substitute
        public Properties getProperties() {
            return new Properties() {
                @Override
                public synchronized Object setProperty(final String key, final String value) {
                    initializeLazyValue(key);
                    return properties.setProperty(key, value);
                }

                @Override
                public synchronized void load(final Reader reader) throws IOException {
                    properties.load(reader);
                }

                @Override
                public synchronized void load(final InputStream inStream) throws IOException {
                    properties.load(inStream);
                }

                @Override
                public void save(final OutputStream out, final String comments) {
                    ensureFullyInitialized();
                    properties.save(out, comments);
                }

                @Override
                public void store(final Writer writer, final String comments) throws IOException {
                    ensureFullyInitialized();
                    properties.store(writer, comments);
                }

                @Override
                public void store(final OutputStream out, final String comments) throws IOException {
                    ensureFullyInitialized();
                    properties.store(out, comments);
                }

                @Override
                public synchronized void loadFromXML(final InputStream in)
                        throws IOException, InvalidPropertiesFormatException {
                    properties.loadFromXML(in);
                }

                @Override
                public void storeToXML(final OutputStream os, final String comment) throws IOException {
                    ensureFullyInitialized();
                    properties.storeToXML(os, comment);
                }

                @Override
                public void storeToXML(final OutputStream os, final String comment, final String encoding)
                        throws IOException {
                    ensureFullyInitialized();
                    properties.storeToXML(os, comment, encoding);
                }

                @Override
                public void storeToXML(final OutputStream os, final String comment, final Charset charset)
                        throws IOException {
                    ensureFullyInitialized();
                    properties.storeToXML(os, comment, charset);
                }

                @Override
                public String getProperty(final String key) {
                    initializeLazyValue(key);
                    return properties.getProperty(key);
                }

                @Override
                public String getProperty(final String key, final String defaultValue) {
                    initializeLazyValue(key);
                    return properties.getProperty(key, defaultValue);
                }

                @Override
                public Enumeration<?> propertyNames() {
                    return properties.propertyNames();
                }

                @Override
                public Set<String> stringPropertyNames() {
                    return properties.stringPropertyNames();
                }

                @Override
                public void list(final PrintStream out) {
                    ensureFullyInitialized();
                    properties.list(out);
                }

                @Override
                public void list(final PrintWriter out) {
                    ensureFullyInitialized();
                    properties.list(out);
                }

                @Override
                public int size() {
                    return properties.size();
                }

                @Override
                public boolean isEmpty() {
                    return properties.isEmpty();
                }

                @Override
                public Enumeration<Object> keys() {
                    return properties.keys();
                }

                @Override
                public Enumeration<Object> elements() {
                    ensureFullyInitialized();
                    return properties.elements();
                }

                @Override
                public boolean contains(final Object value) {
                    ensureFullyInitialized();
                    return properties.contains(value);
                }

                @Override
                public boolean containsValue(final Object value) {
                    ensureFullyInitialized();
                    return properties.containsValue(value);
                }

                @Override
                public boolean containsKey(final Object key) {
                    return properties.containsKey(key);
                }

                @Override
                public Object get(final Object key) {
                    if (key instanceof String) {
                        initializeLazyValue((String) key);
                    }
                    return properties.get(key);
                }

                @Override
                public synchronized Object put(final Object key, final Object value) {
                    if (key instanceof String) {
                        initializeLazyValue((String) key);
                    }
                    return properties.put(key, value);
                }

                @Override
                public synchronized Object remove(final Object key) {
                    if (key instanceof String) {
                        initializeLazyValue((String) key);
                    }
                    return properties.remove(key);
                }

                @Override
                public synchronized void putAll(final Map<?, ?> t) {
                    properties.putAll(t);
                }

                @Override
                public synchronized void clear() {
                    properties.clear();
                }

                @Override
                public synchronized String toString() {
                    ensureFullyInitialized();
                    return properties.toString();
                }

                @Override
                public Set<Object> keySet() {
                    return properties.keySet();
                }

                @Override
                public Collection<Object> values() {
                    ensureFullyInitialized();
                    return properties.values();
                }

                @Override
                public Set<Map.Entry<Object, Object>> entrySet() {
                    ensureFullyInitialized();
                    return properties.entrySet();
                }

                @Override
                public synchronized boolean equals(final Object o) {
                    ensureFullyInitialized();
                    return properties.equals(o);
                }

                @Override
                public synchronized int hashCode() {
                    ensureFullyInitialized();
                    return properties.hashCode();
                }

                @Override
                public Object getOrDefault(final Object key, final Object defaultValue) {
                    if (key instanceof String) {
                        initializeLazyValue((String) key);
                    }
                    return properties.getOrDefault(key, defaultValue);
                }

                @Override
                public synchronized void forEach(final BiConsumer<? super Object, ? super Object> action) {
                    ensureFullyInitialized();
                    properties.forEach(action);
                }

                @Override
                public synchronized void replaceAll(final BiFunction<? super Object, ? super Object, ?> function) {
                    ensureFullyInitialized();
                    properties.replaceAll(function);
                }

                @Override
                public synchronized Object putIfAbsent(final Object key, final Object value) {
                    if (key instanceof String) {
                        initializeLazyValue((String) key);
                    }
                    return properties.putIfAbsent(key, value);
                }

                @Override
                public synchronized boolean remove(final Object key, final Object value) {
                    if (key instanceof String) {
                        initializeLazyValue((String) key);
                    }
                    return properties.remove(key, value);
                }

                @Override
                public synchronized boolean replace(final Object key, final Object oldValue, final Object newValue) {
                    if (key instanceof String) {
                        initializeLazyValue((String) key);
                    }
                    return properties.replace(key, oldValue, newValue);
                }

                @Override
                public synchronized Object replace(final Object key, final Object value) {
                    if (key instanceof String) {
                        initializeLazyValue((String) key);
                    }
                    return properties.replace(key, value);
                }

                @Override
                public synchronized Object computeIfAbsent(
                        final Object key,
                        final Function<? super Object, ?> mappingFunction) {
                    if (key instanceof String) {
                        initializeLazyValue((String) key);
                    }
                    return properties.computeIfAbsent(key, mappingFunction);
                }

                @Override
                public synchronized Object computeIfPresent(
                        final Object key,
                        final BiFunction<? super Object, ? super Object, ?> remappingFunction) {
                    if (key instanceof String) {
                        initializeLazyValue((String) key);
                    }
                    return properties.computeIfPresent(key, remappingFunction);
                }

                @Override
                public synchronized Object compute(
                        final Object key,
                        final BiFunction<? super Object, ? super Object, ?> remappingFunction) {
                    if (key instanceof String) {
                        initializeLazyValue((String) key);
                    }
                    return properties.compute(key, remappingFunction);
                }

                @Override
                public synchronized Object merge(
                        final Object key,
                        final Object value,
                        final BiFunction<? super Object, ? super Object, ?> remappingFunction) {
                    if (key instanceof String) {
                        initializeLazyValue((String) key);
                    }
                    return properties.merge(key, value, remappingFunction);
                }

                @Override
                public synchronized Object clone() {
                    ensureFullyInitialized();
                    return properties.clone();
                }
            };
        }
    }
}
