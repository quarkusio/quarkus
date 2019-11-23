package io.quarkus.runtime.graal;

import org.eclipse.microprofile.config.Config;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.AlwaysInline;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigProviderResolver;

@TargetClass(SmallRyeConfigProviderResolver.class)
final class Target_io_smallrye_config_SmallRyeConfigProviderResolver {
    @Substitute
    public Config getConfig() {
        final SmallRyeConfig config = Target_io_quarkus_runtime_configuration_QuarkusConfigFactory.config;
        if (config == null) {
            throw new IllegalStateException("No configuration is available");
        }
        return config;
    }

    @Substitute
    @AlwaysInline("trivial")
    public Config getConfig(ClassLoader classLoader) {
        return getConfig();
    }

    @Substitute
    public void registerConfig(Config config, ClassLoader classLoader) {
        // no op
    }

    @Substitute
    public void releaseConfig(Config config) {
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
