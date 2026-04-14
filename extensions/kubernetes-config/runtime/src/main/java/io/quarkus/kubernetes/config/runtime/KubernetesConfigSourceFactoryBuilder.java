package io.quarkus.kubernetes.config.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.kubernetes.client.runtime.internal.KubernetesClientConfig;
import io.quarkus.kubernetes.client.runtime.internal.KubernetesClientUtils;
import io.quarkus.runtime.ApplicationLifecycleManager;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class KubernetesConfigSourceFactoryBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        return builder.withSources(new KubernetesConfigFactory());
    }

    static class KubernetesConfigFactory implements ConfigSourceFactory {
        @Override
        public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
            boolean inAppCDsGeneration = Boolean
                    .parseBoolean(System.getProperty(ApplicationLifecycleManager.QUARKUS_APPCDS_GENERATE_PROP, "false"));
            if (inAppCDsGeneration) {
                return Collections.emptyList();
            }

            List<String> profiles = new ArrayList<>(context.getProfiles());
            Collections.reverse(profiles);

            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .withProfiles(profiles)
                    .withSources(new ConfigSourceContext.ConfigSourceContextConfigSource(context))
                    .withSources(context.getConfigSources())
                    .withMapping(KubernetesClientConfig.class)
                    .withValidateUnknown(false)
                    .build();

            KubernetesClientConfig kubernetesClientConfig = config.getConfigMapping(KubernetesClientConfig.class);
            KubernetesClient client = KubernetesClientUtils.createClient(kubernetesClientConfig);
            return new KubernetesConfigSourceFactory(client).getConfigSources(context);
        }
    }
}
