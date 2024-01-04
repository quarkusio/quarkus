package io.quarkus.kubernetes.config.runtime;

import static io.smallrye.config.Converters.getImplicitConverter;

import java.util.Collections;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.arc.runtime.appcds.AppCDSRecorder;
import io.quarkus.kubernetes.client.runtime.KubernetesClientBuildConfig;
import io.quarkus.kubernetes.client.runtime.KubernetesClientUtils;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory.ConfigurableConfigSourceFactory;
import io.smallrye.config.SmallRyeConfigBuilder;

public class KubernetesConfigSourceFactoryBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        return builder.withSources(new KubernetesConfigFactory());
    }

    static class KubernetesConfigFactory implements ConfigurableConfigSourceFactory<KubernetesClientBuildConfig> {
        @Override
        public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context,
                final KubernetesClientBuildConfig config) {
            boolean inAppCDsGeneration = Boolean
                    .parseBoolean(System.getProperty(AppCDSRecorder.QUARKUS_APPCDS_GENERATE_PROP, "false"));
            if (inAppCDsGeneration) {
                return Collections.emptyList();
            }

            // TODO - TlsConfig is used in a lot of place. This is to avoid having it to migrate to ConfigMapping.
            boolean trustAll = getImplicitConverter(Boolean.class)
                    .convert(context.getValue("quarkus.tls.trust-all").getValue());
            TlsConfig tlsConfig = new TlsConfig();
            tlsConfig.trustAll = trustAll;
            KubernetesClient client = KubernetesClientUtils.createClient(config, tlsConfig);
            return new KubernetesConfigSourceFactory(client).getConfigSources(context);
        }
    }
}
