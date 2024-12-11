package io.quarkus.kubernetes.deployment;

import static io.smallrye.config.ConfigMappingInterface.getProperties;
import static io.smallrye.config.ConfigMappingLoader.getConfigMapping;
import static io.smallrye.config.ConfigValue.CONFIG_SOURCE_COMPARATOR;

import java.util.HashSet;
import java.util.Iterator;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.FallbackConfigSourceInterceptor;
import io.smallrye.config.Priorities;
import io.smallrye.config.PropertyName;
import io.smallrye.config.RelocateConfigSourceInterceptor;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

public class KubernetesConfigBuilderCustomizer implements SmallRyeConfigBuilderCustomizer {
    private static final Set<PropertyName> IGNORE_OPENSHIFT_NAMES = ignoreOpenshiftNames();
    private static final Set<PropertyName> IGNORE_KNATIVE_NAMES = ignoreKnativeNames();

    @Override
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        builder.withInterceptorFactories(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                return new Fallbacks(new Function<String, String>() {
                    @Override
                    public String apply(final String name) {
                        if (name.startsWith("quarkus.openshift.") && !IGNORE_OPENSHIFT_NAMES.contains(new PropertyName(name))) {
                            return "quarkus.kubernetes." + name.substring(18);
                        } else if (name.startsWith("quarkus.knative.")
                                && !IGNORE_KNATIVE_NAMES.contains(new PropertyName(name))) {
                            return "quarkus.kubernetes." + name.substring(16);
                        }
                        return name;
                    }
                }) {
                    // There is no bidirectional connection between kubernetes and openshift / knative, so do not
                    // include reverse properties
                    @Override
                    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
                        return context.iterateNames();
                    }
                };
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 590);
            }
        });
        // To rewrite the fallback names to the main name. Required for fallbacks to work properly with Maps keys
        builder.withInterceptorFactories(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                return new RelocateConfigSourceInterceptor(new Function<String, String>() {
                    @Override
                    public String apply(final String name) {
                        if (name.startsWith("quarkus.kubernetes.")
                                && !IGNORE_OPENSHIFT_NAMES.contains(new PropertyName(name))) {
                            return "quarkus.openshift." + name.substring(19);
                        }
                        return name;
                    }
                }) {
                    @Override
                    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
                        return context.proceed(name);
                    }
                };
            }
        });
        // To rewrite the fallback names to the main name. Required for fallbacks to work properly with Maps keys
        builder.withInterceptorFactories(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                return new RelocateConfigSourceInterceptor(new Function<String, String>() {
                    @Override
                    public String apply(final String name) {
                        if (name.startsWith("quarkus.kubernetes.") && !IGNORE_KNATIVE_NAMES.contains(new PropertyName(name))) {
                            return "quarkus.knative." + name.substring(19);
                        }
                        return name;
                    }
                }) {
                    @Override
                    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
                        return context.proceed(name);
                    }
                };
            }
        });
    }

    /**
     * Overrides the base {@link FallbackConfigSourceInterceptor} to use {@link ConfigSourceInterceptorContext#restart}
     * instead of {@link ConfigSourceInterceptorContext#proceed}. The plan is to move the base one to use it as well,
     * but it is a breaking change so it is better to keep it locally here for now.
     */
    private static class Fallbacks extends FallbackConfigSourceInterceptor {
        public Fallbacks(final Function<String, String> mapping) {
            super(mapping);
        }

        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            ConfigValue configValue = context.proceed(name);
            String map = getMapping().apply(name);

            if (name.equals(map)) {
                return configValue;
            }

            ConfigValue fallbackValue = context.restart(map);
            // Check which one comes from a higher ordinal source to avoid defaults from the main name
            if (configValue != null && fallbackValue != null) {
                return CONFIG_SOURCE_COMPARATOR.compare(configValue, fallbackValue) >= 0 ? configValue
                        : fallbackValue.withName(name);
            }

            if (configValue != null) {
                return configValue;
            } else if (fallbackValue != null) {
                return fallbackValue.withName(name);
            }
            return null;
        }
    }

    /**
     * Collect the properties names that are not shared between <code>kubernetes</code> and <code>openshift</code>
     * to ignore when performing the fallback functions.
     *
     * @return a Set of properties names to ignore
     */
    private static Set<PropertyName> ignoreOpenshiftNames() {
        Set<String> kubernetes = getProperties(getConfigMapping(KubernetesConfig.class)).get(KubernetesConfig.class).get("")
                .keySet();
        Set<String> openshift = getProperties(getConfigMapping(OpenShiftConfig.class)).get(OpenShiftConfig.class).get("")
                .keySet();

        Set<PropertyName> ignored = new HashSet<>();
        for (String name : kubernetes) {
            if (!openshift.contains(name)) {
                ignored.add(new PropertyName("quarkus.kubernetes." + name));
                ignored.add(new PropertyName("quarkus.openshift." + name));
            }
        }

        for (String name : openshift) {
            if (!kubernetes.contains(name)) {
                ignored.add(new PropertyName("quarkus.kubernetes." + name));
                ignored.add(new PropertyName("quarkus.openshift." + name));
            }
        }

        // These are shared, but must work independently
        ignored.add(new PropertyName("quarkus.kubernetes.deploy"));
        ignored.add(new PropertyName("quarkus.openshift.deploy"));
        ignored.add(new PropertyName("quarkus.kubernetes.deploy-strategy"));
        ignored.add(new PropertyName("quarkus.openshift.deploy-strategy"));
        return ignored;
    }

    /**
     * Collect the properties names that are not shared between <code>kubernetes</code> and <code>knative</code> to
     * ignore when performing the fallback functions.
     *
     * @return a Set of properties names to ignore
     */
    private static Set<PropertyName> ignoreKnativeNames() {
        Set<String> kubernetes = getProperties(getConfigMapping(KubernetesConfig.class)).get(KubernetesConfig.class).get("")
                .keySet();
        Set<String> knative = getProperties(getConfigMapping(KnativeConfig.class)).get(KnativeConfig.class).get("").keySet();

        Set<PropertyName> ignored = new HashSet<>();
        for (String name : kubernetes) {
            if (!knative.contains(name)) {
                ignored.add(new PropertyName("quarkus.kubernetes." + name));
                ignored.add(new PropertyName("quarkus.openshift." + name));
            }
        }

        for (String name : knative) {
            if (!kubernetes.contains(name)) {
                ignored.add(new PropertyName("quarkus.kubernetes." + name));
                ignored.add(new PropertyName("quarkus.openshift." + name));
            }
        }

        // These are shared, but must work independently
        ignored.add(new PropertyName("quarkus.kubernetes.deploy"));
        ignored.add(new PropertyName("quarkus.knative.deploy"));
        ignored.add(new PropertyName("quarkus.kubernetes.deploy-strategy"));
        ignored.add(new PropertyName("quarkus.knative.deploy-strategy"));
        return ignored;
    }
}
