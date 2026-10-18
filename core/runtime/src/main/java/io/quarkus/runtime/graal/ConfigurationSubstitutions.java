package io.quarkus.runtime.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.smallrye.config.Config;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigProviderResolver;

@TargetClass(SmallRyeConfigProviderResolver.class)
final class Target_io_smallrye_config_SmallRyeConfigProviderResolver {
    @Substitute
    public Config getConfig() {
        return get();
    }

    @Substitute
    public Config getConfig(ClassLoader classLoader) {
        return get();
    }

    @Substitute
    public SmallRyeConfig get() {
        SmallRyeConfig config = Target_io_quarkus_runtime_configuration_QuarkusConfigFactory.config;
        if (config == null) {
            throw new IllegalStateException("No configuration is available");
        }
        return config;
    }

    @Substitute
    public SmallRyeConfig get(ClassLoader classLoader) {
        return get();
    }

    @Substitute
    public void registerConfig(org.eclipse.microprofile.config.Config config, ClassLoader classLoader) {
        // no op
    }

    @Substitute
    public void releaseConfig(org.eclipse.microprofile.config.Config config) {
        // no op
    }

    @Substitute
    public void releaseConfig(ClassLoader classLoader) {
        // no op
    }
}

@TargetClass(QuarkusConfigFactory.class)
final class Target_io_quarkus_runtime_configuration_QuarkusConfigFactory {
    @Alias
    static SmallRyeConfig config;
}

final class ConfigurationSubstitutions {
}
