package io.quarkus.hibernate.search.orm.elasticsearch.aws.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.amazon.common.runtime.AwsCredentialsProviderConfig;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import software.amazon.awssdk.regions.Region;

@ConfigGroup
public class HibernateSearchOrmElasticsearchAwsRuntimeConfigPersistenceUnit {

    /**
     * Default backend
     */
    @ConfigItem(name = "elasticsearch")
    ElasticsearchBackendRuntimeConfig defaultBackend;

    /**
     * Named backends
     */
    @ConfigItem(name = "elasticsearch")
    public ElasticsearchNamedBackendsRuntimeConfig namedBackends;

    @ConfigGroup
    public static class ElasticsearchNamedBackendsRuntimeConfig {

        /**
         * Named backends
         */
        @ConfigDocMapKey("backend-name")
        public Map<String, ElasticsearchBackendRuntimeConfig> backends;

    }

    @ConfigGroup
    public static class ElasticsearchBackendRuntimeConfig {

        /**
         * AWS services configurations
         */
        @ConfigItem
        ElasticsearchBackendAwsConfig aws;

    }

    @ConfigGroup
    public static class ElasticsearchBackendAwsConfig {

        /**
         * Whether requests should be signed using the AWS credentials.
         */
        @ConfigItem(name = "signing.enabled")
        boolean signingEnabled;

        // @formatter:off
        /**
         * An Amazon Web Services region that hosts the Elasticsearch service.
         *
         * Must be provided if signing is enabled; the region won't be automatically detected.
         *
         * See `software.amazon.awssdk.regions.Region` for available regions.
         *
         * @asciidoclet
         */
        // @formatter:on
        @ConfigItem
        Optional<Region> region;

        /**
         * Defines the credentials provider.
         */
        @ConfigItem
        AwsCredentialsProviderConfig credentials;

    }

}
