package io.quarkus.deployment.configuration;

import static java.util.Collections.emptyIterator;
import static java.util.Map.entry;

import java.io.Serial;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiFunction;

import jakarta.annotation.Priority;

import org.jboss.logging.Logger;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.KeyMap;
import io.smallrye.config.NameIterator;

/**
 * A class which manages configuration compatibility for the build-time configuration.
 * The mechanism is to keep a matching table of old names and new names.
 * Old names are detected at the front end iterator and mapped to the corresponding new name(s) that replace them.
 * New names are detected at the back end getter, which queries the corresponding old name(s) to determine the new value.
 * <p>
 * This is intended to be a temporary and evolutionary solution to be replaced by generative remapping.
 * It is more flexible than fallback remapping, allowing 1:N and N:1 remappings and support for submaps.
 * Unfortunately it is also very verbose.
 */
public final class ConfigCompatibility {

    private static final Logger log = Logger.getLogger("io.quarkus.deployment.configuration");

    /**
     * When these legacy name patterns are detected on iteration, remove them or replace them with other name(s).
     */
    private static final KeyMap<BiFunction<ConfigSourceInterceptorContext, NameIterator, List<String>>> oldNames = keyMap(
            entry(List.of("quarkus", "package", "type"), ConfigCompatibility::quarkusPackageType),
            entry(List.of("quarkus", "package", "create-appcds"), ConfigCompatibility::quarkusPackageCreateAppcds),
            entry(List.of("quarkus", "package", "appcds-builder-image"), ConfigCompatibility::quarkusPackageAppcdsBuilderImage),
            entry(List.of("quarkus", "package", "appcds-use-container"), ConfigCompatibility::quarkusPackageAppcdsUseContainer),
            entry(List.of("quarkus", "package", "compress-jar"), ConfigCompatibility::quarkusPackageCompressJar),
            entry(List.of("quarkus", "package", "filter-optional-dependencies"),
                    ConfigCompatibility::quarkusFilterOptionalDependencies),
            entry(List.of("quarkus", "package", "add-runner-suffix"), ConfigCompatibility::quarkusPackageAddRunnerSuffix),
            entry(List.of("quarkus", "package", "user-configured-ignored-entries"),
                    ConfigCompatibility::quarkusPackageUserConfiguredIgnoredEntries),
            entry(List.of("quarkus", "package", "user-providers-directory"),
                    ConfigCompatibility::quarkusPackageUserProvidersDirectory),
            entry(List.of("quarkus", "package", "included-optional-dependencies"),
                    ConfigCompatibility::quarkusPackageIncludedOptionalDependencies),
            entry(List.of("quarkus", "package", "include-dependency-list"),
                    ConfigCompatibility::quarkusPackageIncludeDependencyList),
            entry(List.of("quarkus", "package", "decompiler", "version"),
                    ConfigCompatibility::quarkusPackageDecompilerVersion),
            entry(List.of("quarkus", "package", "decompiler", "enabled"),
                    ConfigCompatibility::quarkusPackageDecompilerEnabled),
            entry(List.of("quarkus", "package", "decompiler", "jar-directory"),
                    ConfigCompatibility::quarkusPackageDecompilerJarDirectory),
            entry(List.of("quarkus", "package", "manifest", "attributes", "*"),
                    ConfigCompatibility::quarkusPackageManifestAttributes),
            entry(List.of("quarkus", "package", "manifest", "sections", "*", "*"),
                    ConfigCompatibility::quarkusPackageManifestSections),
            entry(List.of("quarkus", "package", "manifest", "add-implementation-entries"),
                    ConfigCompatibility::quarkusPackageManifestAddImplementationEntries));

    /**
     * When these new name patterns are detected on get, see if legacy values are present and if so,
     * provide a default based on those value(s).
     */
    public static final KeyMap<BiFunction<ConfigSourceInterceptorContext, NameIterator, ConfigValue>> newNames = keyMap(
            entry(List.of("quarkus", "native", "enabled"), ConfigCompatibility::quarkusNativeEnabled),
            entry(List.of("quarkus", "native", "sources-only"), ConfigCompatibility::quarkusNativeSourcesOnly),
            entry(List.of("quarkus", "package", "jar", "enabled"), ConfigCompatibility::quarkusPackageJarEnabled),
            entry(List.of("quarkus", "package", "jar", "appcds", "enabled"),
                    ConfigCompatibility::quarkusPackageJarAppcdsEnabled),
            entry(List.of("quarkus", "package", "jar", "appcds", "builder-image"),
                    ConfigCompatibility::quarkusPackageJarAppcdsBuilderImage),
            entry(List.of("quarkus", "package", "jar", "appcds", "use-container"),
                    ConfigCompatibility::quarkusPackageJarAppcdsUseContainer),
            entry(List.of("quarkus", "package", "jar", "type"), ConfigCompatibility::quarkusPackageJarType),
            entry(List.of("quarkus", "package", "jar", "compress"), ConfigCompatibility::quarkusPackageJarCompress),
            entry(List.of("quarkus", "package", "jar", "filter-optional-dependencies"),
                    ConfigCompatibility::quarkusPackageJarFilterOptionalDependencies),
            entry(List.of("quarkus", "package", "jar", "add-runner-suffix"),
                    ConfigCompatibility::quarkusPackageJarAddRunnerSuffix),
            entry(List.of("quarkus", "package", "jar", "user-configured-ignored-entries"),
                    ConfigCompatibility::quarkusPackageJarUserConfiguredIgnoredEntries),
            entry(List.of("quarkus", "package", "jar", "user-providers-directory"),
                    ConfigCompatibility::quarkusPackageJarUserProvidersDirectory),
            entry(List.of("quarkus", "package", "jar", "included-optional-dependencies"),
                    ConfigCompatibility::quarkusPackageJarIncludedOptionalDependencies),
            entry(List.of("quarkus", "package", "jar", "include-dependency-list"),
                    ConfigCompatibility::quarkusPackageJarIncludeDependencyList),
            entry(List.of("quarkus", "package", "jar", "manifest", "attributes", "*"),
                    ConfigCompatibility::quarkusPackageJarManifestAttributes),
            entry(List.of("quarkus", "package", "jar", "manifest", "sections", "*", "*"),
                    ConfigCompatibility::quarkusPackageJarManifestSections),
            entry(List.of("quarkus", "package", "jar", "manifest", "add-implementation-entries"),
                    ConfigCompatibility::quarkusPackageJarManifestAddImplementationEntries),
            entry(List.of("quarkus", "package", "jar", "decompiler", "enabled"),
                    ConfigCompatibility::quarkusPackageJarDecompilerEnabled),
            entry(List.of("quarkus", "package", "jar", "decompiler", "jar-directory"),
                    ConfigCompatibility::quarkusPackageJarDecompilerJarDirectory));

    /**
     * The interceptor at the front of the chain which handles hiding deprecated properties from the iterator.
     */
    @Priority(Integer.MAX_VALUE)
    public static final class FrontEnd implements ConfigSourceInterceptor {
        @Serial
        private static final long serialVersionUID = -3438497970389074611L;

        private static final FrontEnd instance = new FrontEnd(true);
        private static final FrontEnd nonLoggingInstance = new FrontEnd(false);

        private final boolean logging;

        private FrontEnd(final boolean logging) {
            this.logging = logging;
        }

        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            return context.proceed(name);
        }

        public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
            Iterator<String> nextIter = context.iterateNames();
            return new Iterator<String>() {
                /**
                 * Replacement names iterator.
                 */
                Iterator<String> subIter = emptyIterator();
                /**
                 * The next value.
                 */
                String next;

                public boolean hasNext() {
                    while (next == null) {
                        if (subIter.hasNext()) {
                            // more replacement names remain
                            next = subIter.next();
                            return true;
                        }
                        if (!nextIter.hasNext()) {
                            return false;
                        }
                        String next = nextIter.next();
                        var fn = oldNames.findRootValue(next);
                        if (fn == null) {
                            // it's not a deprecated name, so we can return it as-is
                            this.next = next;
                            return true;
                        }
                        // get the replacement names
                        List<String> list = fn.apply(context, new NameIterator(next));
                        subIter = list.iterator();
                        if (logging) {
                            // todo: print these warnings when mapping the configuration so they cannot appear more than once
                            if (list.isEmpty()) {
                                log.warnf("Configuration property '%s' has been deprecated and will be ignored", next);
                            } else {
                                log.warnf("Configuration property '%s' has been deprecated and replaced by: %s", next, list);
                            }
                        }
                    }
                    return true;
                }

                public String next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    String next = this.next;
                    this.next = null;
                    return next;
                }
            };
        }

        public static FrontEnd instance() {
            return instance;
        }

        public static FrontEnd nonLoggingInstance() {
            return nonLoggingInstance;
        }
    }

    /**
     * The interceptor at the back of the chain which provides compatibility defaults for new property names.
     */
    @Priority(Integer.MIN_VALUE + 1)
    public static final class BackEnd implements ConfigSourceInterceptor {
        @Serial
        private static final long serialVersionUID = 6840768821115677665L;

        private static final BackEnd instance = new BackEnd();

        private BackEnd() {
        }

        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            NameIterator ni = new NameIterator(name);
            BiFunction<ConfigSourceInterceptorContext, NameIterator, ConfigValue> function = newNames.findRootValue(ni);
            return function != null ? function.apply(context, ni) : context.proceed(name);
        }

        public static BackEnd instance() {
            return instance;
        }
    }

    // front end mappings here

    private static List<String> quarkusPackageType(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        // check the value to see what properties we need to define
        ConfigValue legacyPackageType = ctxt.proceed(ni.getName());
        if (legacyPackageType == null) {
            // nothing to do
            return List.of();
        }
        // override defaults of all of these properties
        return List.of("quarkus.package.jar.enabled", "quarkus.package.jar.type", "quarkus.native.enabled",
                "quarkus.native.sources-only");
    }

    private static List<String> quarkusPackageCreateAppcds(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        return List.of("quarkus.package.jar.appcds.enabled");
    }

    private static List<String> quarkusPackageAppcdsBuilderImage(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        return List.of("quarkus.package.jar.appcds.builder-image");
    }

    private static List<String> quarkusPackageAppcdsUseContainer(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        return List.of("quarkus.package.jar.appcds.use-container");
    }

    private static List<String> quarkusPackageCompressJar(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        return List.of("quarkus.package.jar.compress");
    }

    private static List<String> quarkusFilterOptionalDependencies(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        return List.of("quarkus.package.jar.filter-optional-dependencies");
    }

    private static List<String> quarkusPackageAddRunnerSuffix(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        return List.of("quarkus.package.jar.add-runner-suffix");
    }

    private static List<String> quarkusPackageUserConfiguredIgnoredEntries(ConfigSourceInterceptorContext ctxt,
            NameIterator ni) {
        return List.of("quarkus.package.jar.user-configured-ignored-entries");
    }

    private static List<String> quarkusPackageIncludeDependencyList(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        return List.of("quarkus.package.jar.include-dependency-list");
    }

    private static List<String> quarkusPackageUserProvidersDirectory(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        return List.of("quarkus.package.jar.user-providers-directory");
    }

    private static List<String> quarkusPackageIncludedOptionalDependencies(ConfigSourceInterceptorContext ctxt,
            NameIterator ni) {
        return List.of("quarkus.package.jar.included-optional-dependencies");
    }

    private static List<String> quarkusPackageDecompilerVersion(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        // always hide this ignored property
        return List.of();
    }

    private static List<String> quarkusPackageDecompilerEnabled(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        // simple mapping to a new name
        return List.of("quarkus.package.jar.decompiler.enabled");
    }

    private static List<String> quarkusPackageDecompilerJarDirectory(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        // simple mapping to a new name
        return List.of("quarkus.package.jar.decompiler.jar-directory");
    }

    private static List<String> quarkusPackageManifestAttributes(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        // mapping to a new name, copying the last segment
        ni.goToEnd();
        ni.previous();
        return List.of("quarkus.package.jar.manifest.attributes." + ni.getName().substring(ni.getPosition() + 1));
    }

    private static List<String> quarkusPackageManifestSections(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        // mapping to a new name, copying the last two segments
        ni.goToEnd();
        ni.previous();
        ni.previous();
        return List.of("quarkus.package.jar.manifest.sections." + ni.getName().substring(ni.getPosition() + 1));
    }

    private static List<String> quarkusPackageManifestAddImplementationEntries(ConfigSourceInterceptorContext ctxt,
            NameIterator ni) {
        // simple mapping to a new name
        return List.of("quarkus.package.jar.manifest.add-implementation-entries");
    }

    // back end mappings here

    private static final Set<String> ANY_NATIVE = Set.of("native", "native-sources");

    private static ConfigValue quarkusNativeEnabled(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        // GraalVM native image is enabled if the legacy package type is "native" or "native sources"
        ConfigValue ptVal = ctxt.restart("quarkus.package.type");
        if (ptVal == null) {
            // on to the default value
            return ctxt.proceed(ni.getName());
        } else {
            // map old name to new name
            return ptVal.withName(ni.getName()).withValue(
                    Boolean.toString(ANY_NATIVE.contains(ptVal.getValue())));
        }
    }

    private static ConfigValue quarkusPackageJarEnabled(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        // the JAR packaging type is present if a JAR type was configured in the legacy property
        ConfigValue ptVal = ctxt.restart("quarkus.package.type");
        if (ptVal == null) {
            // on to the default value
            return ctxt.proceed(ni.getName());
        } else {
            return ptVal.withName(ni.getName()).withValue(
                    Boolean.toString(!ANY_NATIVE.contains(ptVal.getValue())));
        }
    }

    private static ConfigValue quarkusPackageJarAppcdsEnabled(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        ConfigValue oldVal = ctxt.restart("quarkus.package.create-appcds");
        if (oldVal == null) {
            // on to the default value
            return ctxt.proceed(ni.getName());
        } else {
            return oldVal.withName(ni.getName());
        }
    }

    private static ConfigValue quarkusPackageJarAppcdsBuilderImage(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        ConfigValue oldVal = ctxt.restart("quarkus.package.appcds-builder-image");
        if (oldVal == null) {
            // on to the default value
            return ctxt.proceed(ni.getName());
        } else {
            return oldVal.withName(ni.getName());
        }
    }

    private static ConfigValue quarkusPackageJarAppcdsUseContainer(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        ConfigValue oldVal = ctxt.restart("quarkus.package.appcds-use-container");
        if (oldVal == null) {
            // on to the default value
            return ctxt.proceed(ni.getName());
        } else {
            return oldVal.withName(ni.getName());
        }
    }

    private static ConfigValue quarkusPackageJarType(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        ConfigValue ptVal = ctxt.restart("quarkus.package.type");
        if (ptVal == null) {
            // on to the default value
            return ctxt.proceed(ni.getName());
        } else {
            return ANY_NATIVE.contains(ptVal.getValue()) ? ctxt.proceed(ni.getName()) : ptVal.withName(ni.getName());
        }
    }

    private static ConfigValue quarkusPackageJarCompress(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        ConfigValue oldVal = ctxt.restart("quarkus.package.compress-jar");
        if (oldVal == null) {
            // on to the default value
            return ctxt.proceed(ni.getName());
        } else {
            return oldVal.withName(ni.getName());
        }
    }

    private static ConfigValue quarkusPackageJarFilterOptionalDependencies(ConfigSourceInterceptorContext ctxt,
            NameIterator ni) {
        ConfigValue oldVal = ctxt.restart("quarkus.package.filter-optional-dependencies");
        if (oldVal == null) {
            // on to the default value
            return ctxt.proceed(ni.getName());
        } else {
            return oldVal.withName(ni.getName());
        }
    }

    private static ConfigValue quarkusPackageJarAddRunnerSuffix(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        ConfigValue oldVal = ctxt.restart("quarkus.package.add-runner-suffix");
        if (oldVal == null) {
            // on to the default value
            return ctxt.proceed(ni.getName());
        } else {
            return oldVal.withName(ni.getName());
        }
    }

    private static ConfigValue quarkusPackageJarUserConfiguredIgnoredEntries(ConfigSourceInterceptorContext ctxt,
            NameIterator ni) {
        ConfigValue oldVal = ctxt.restart("quarkus.package.user-configured-ignored-entries");
        if (oldVal == null) {
            // on to the default value
            return ctxt.proceed(ni.getName());
        } else {
            return oldVal.withName(ni.getName());
        }
    }

    private static ConfigValue quarkusPackageJarUserProvidersDirectory(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        ConfigValue oldVal = ctxt.restart("quarkus.package.user-providers-directory");
        if (oldVal == null) {
            // on to the default value
            return ctxt.proceed(ni.getName());
        } else {
            return oldVal.withName(ni.getName());
        }
    }

    private static ConfigValue quarkusPackageJarIncludedOptionalDependencies(ConfigSourceInterceptorContext ctxt,
            NameIterator ni) {
        ConfigValue oldVal = ctxt.restart("quarkus.package.included-optional-dependencies");
        if (oldVal == null) {
            // on to the default value
            return ctxt.proceed(ni.getName());
        } else {
            return oldVal.withName(ni.getName());
        }
    }

    private static ConfigValue quarkusPackageJarIncludeDependencyList(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        ConfigValue oldVal = ctxt.restart("quarkus.package.include-dependency-list");
        if (oldVal == null) {
            // on to the default value
            return ctxt.proceed(ni.getName());
        } else {
            return oldVal.withName(ni.getName());
        }
    }

    private static ConfigValue quarkusNativeSourcesOnly(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        // GraalVM native image is enabled if the legacy package type is "native" or "native sources"
        ConfigValue ptVal = ctxt.restart("quarkus.package.type");
        if (ptVal == null) {
            // on to the default value
            return ctxt.proceed(ni.getName());
        } else {
            // map old name to new name
            return ptVal.withName(ni.getName()).withValue(Boolean.toString(ptVal.getValue().equals("native-sources")));
        }
    }

    private static ConfigValue quarkusPackageJarManifestAttributes(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        // mapping from a legacy name, copying the last segment
        ni.goToEnd();
        ni.previous();
        String oldName = "quarkus.package.manifest.attributes." + ni.getName().substring(ni.getPosition() + 1);
        ConfigValue oldVal = ctxt.restart(oldName);
        if (oldVal == null) {
            return ctxt.proceed(ni.getName());
        } else {
            return oldVal.withName(ni.getName());
        }
    }

    private static ConfigValue quarkusPackageJarManifestSections(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        // mapping from a legacy name, copying the last two segments
        ni.goToEnd();
        ni.previous();
        ni.previous();
        String oldName = "quarkus.package.manifest.sections." + ni.getName().substring(ni.getPosition() + 1);
        ConfigValue oldVal = ctxt.restart(oldName);
        if (oldVal == null) {
            return ctxt.proceed(ni.getName());
        } else {
            return oldVal.withName(ni.getName());
        }
    }

    private static ConfigValue quarkusPackageJarManifestAddImplementationEntries(ConfigSourceInterceptorContext ctxt,
            NameIterator ni) {
        ConfigValue oldVal = ctxt.restart("quarkus.package.manifest.add-implementation-entries");
        if (oldVal == null) {
            // on to the default value
            return ctxt.proceed(ni.getName());
        } else {
            // map old name to new name
            return oldVal.withName(ni.getName());
        }
    }

    private static ConfigValue quarkusPackageJarDecompilerEnabled(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        ConfigValue oldVal = ctxt.restart("quarkus.package.decompiler.enabled");
        if (oldVal == null) {
            return ctxt.proceed(ni.getName());
        }
        // map old name to new name
        return oldVal.withName(ni.getName());
    }

    private static ConfigValue quarkusPackageJarDecompilerJarDirectory(ConfigSourceInterceptorContext ctxt, NameIterator ni) {
        ConfigValue oldVal = ctxt.restart("quarkus.package.decompiler.jar-directory");
        if (oldVal == null) {
            return ctxt.proceed(ni.getName());
        }
        // map old name to new name
        return oldVal.withName(ni.getName());
    }

    // utilities

    @SafeVarargs
    private static <T> KeyMap<T> keyMap(
            Map.Entry<List<String>, T>... entries) {
        KeyMap<T> keyMap = new KeyMap<>();
        KeyMap<T> subMap;
        for (Map.Entry<List<String>, T> entry : entries) {
            subMap = keyMap;
            for (String part : entry.getKey()) {
                if (part.equals("*")) {
                    subMap = subMap.getOrCreateAny();
                } else {
                    KeyMap<T> tryMap = subMap.get(part);
                    if (tryMap == null) {
                        tryMap = new KeyMap<>();
                        subMap.put(part, tryMap);
                    }
                    subMap = tryMap;
                }
            }
            subMap.putRootValue(entry.getValue());
        }
        return keyMap;
    }
}
