package io.quarkus.hibernate.search.orm.elasticsearch.aws.runtime;

import static io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchConfigUtil.addBackendConfig;

import java.util.Map.Entry;
import java.util.function.BiConsumer;

import org.hibernate.search.backend.elasticsearch.aws.cfg.ElasticsearchAwsBackendSettings;
import org.hibernate.search.backend.elasticsearch.aws.spi.ElasticsearcAwsCredentialsProvider;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;
import io.quarkus.hibernate.search.orm.elasticsearch.aws.runtime.HibernateSearchOrmElasticsearchAwsRuntimeConfigPersistenceUnit.ElasticsearchBackendRuntimeConfig;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

@Recorder
public class HibernateSearchOrmElasticsearchAwsRecorder {

    public HibernateOrmIntegrationRuntimeInitListener createRuntimeInitListener(
            HibernateSearchOrmElasticsearchAwsRuntimeConfig runtimeConfig, String persistenceUnitName) {
        HibernateSearchOrmElasticsearchAwsRuntimeConfigPersistenceUnit puConfig = PersistenceUnitUtil
                .isDefaultPersistenceUnit(persistenceUnitName)
                        ? runtimeConfig.defaultPersistenceUnit
                        : runtimeConfig.persistenceUnits.get(persistenceUnitName);
        if (puConfig == null) {
            return null;
        }
        return new RuntimeInitListener(puConfig);
    }

    private static final class RuntimeInitListener
            implements HibernateOrmIntegrationRuntimeInitListener {

        private final HibernateSearchOrmElasticsearchAwsRuntimeConfigPersistenceUnit runtimeConfig;

        private RuntimeInitListener(
                HibernateSearchOrmElasticsearchAwsRuntimeConfigPersistenceUnit runtimeConfig) {
            this.runtimeConfig = runtimeConfig;
        }

        @Override
        public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
            contributeBackendRuntimeProperties(propertyCollector, null,
                    runtimeConfig.defaultBackend);

            for (Entry<String, ElasticsearchBackendRuntimeConfig> backendEntry : runtimeConfig.namedBackends.backends
                    .entrySet()) {
                contributeBackendRuntimeProperties(propertyCollector, backendEntry.getKey(), backendEntry.getValue());
            }
        }

        private void contributeBackendRuntimeProperties(BiConsumer<String, Object> propertyCollector, String backendName,
                ElasticsearchBackendRuntimeConfig elasticsearchBackendConfig) {
            HibernateSearchOrmElasticsearchAwsRuntimeConfigPersistenceUnit.ElasticsearchBackendAwsConfig aws = elasticsearchBackendConfig.aws;
            if (aws == null || !aws.signingEnabled) {
                return;
            }

            addBackendConfig(propertyCollector, backendName, ElasticsearchAwsBackendSettings.SIGNING_ENABLED,
                    true);

            String configKeyRoot = backendName == null
                    ? "quarkus.hibernate-search-orm.elasticsearch"
                    : "quarkus.hibernate-search-orm.elasticsearch.backends." + backendName;

            if (!aws.region.isPresent()) {
                String propertyKey = configKeyRoot + ".aws.region";
                throw new RuntimeException(
                        "When AWS request signing is enabled, the AWS region needs to be defined via property '"
                                + propertyKey + "'.");
            }
            addBackendConfig(propertyCollector, backendName, ElasticsearchAwsBackendSettings.REGION,
                    aws.region.get().id());

            AwsCredentialsProvider credentialProvider = aws.credentials.type.create(aws.credentials, configKeyRoot);
            addBackendConfig(propertyCollector, backendName, ElasticsearchAwsBackendSettings.CREDENTIALS_TYPE,
                    (ElasticsearcAwsCredentialsProvider) configurationPropertySource -> credentialProvider);
        }

    }
}
