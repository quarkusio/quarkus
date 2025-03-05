package io.quarkus.elasticsearch.restclient.common.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.elasticsearch")
public interface ElasticsearchCommonBuildTimeConfig {

    /**
     * Dev Services
     * <p>
     * Dev Services allows Quarkus to automatically start Elasticsearch in dev and test mode.
     */
    @ConfigDocSection(generated = true)
    ElasticsearchDevServicesBuildTimeConfig devservices();

    @ConfigGroup
    interface ElasticsearchDevServicesBuildTimeConfig {
        /**
         * Whether this Dev Service should start with the application in dev mode or tests.
         *
         * Dev Services are enabled by default
         * unless connection configuration (e.g. `quarkus.elasticsearch.hosts`) is set explicitly.
         *
         * @asciidoclet
         */
        Optional<Boolean> enabled();

        /**
         * Optional fixed port the dev service will listen to.
         * <p>
         * If not defined, the port will be chosen randomly.
         */
        Optional<Integer> port();

        /**
         * The Elasticsearch distribution to use.
         *
         * Defaults to a distribution inferred from the explicitly configured `image-name` (if any),
         * or by default to the distribution configured in depending extensions (e.g. Hibernate Search),
         * or by default to `elastic`.
         *
         * @asciidoclet
         */
        Optional<Distribution> distribution();

        /**
         * The Elasticsearch container image to use.
         *
         * Defaults depend on the configured `distribution`:
         *
         * * For the `elastic` distribution: {elasticsearch-image}
         * * For the `opensearch` distribution: {opensearch-image}
         *
         * @asciidoclet
         */
        Optional<String> imageName();

        /**
         * The value for the ES_JAVA_OPTS env variable.
         *
         * @asciidoclet
         */
        @WithDefault("-Xms512m -Xmx1g")
        String javaOpts();

        /**
         * Whether the Elasticsearch server managed by Quarkus Dev Services is shared.
         * <p>
         * When shared, Quarkus looks for running containers using label-based service discovery.
         * If a matching container is found, it is used, and so a second one is not started.
         * Otherwise, Dev Services for Elasticsearch starts a new container.
         * <p>
         * The discovery uses the {@code quarkus-dev-service-elasticsearch} label.
         * The value is configured using the {@code service-name} property.
         * <p>
         * Container sharing is only used in dev mode.
         */
        @WithDefault("true")
        boolean shared();

        /**
         * The value of the {@code quarkus-dev-service-elasticsearch} label attached to the started container.
         * <p>
         * This property is used when {@code shared} is set to {@code true}.
         * In this case, before starting a container, Dev Services for Elasticsearch looks for a container with the
         * {@code quarkus-dev-service-elasticsearch} label
         * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise it
         * starts a new container with the {@code quarkus-dev-service-elasticsearch} label set to the specified value.
         * <p>
         * This property is used when you need multiple shared Elasticsearch servers.
         */
        @WithDefault("elasticsearch")
        String serviceName();

        /**
         * Environment variables that are passed to the container.
         */
        @ConfigDocMapKey("environment-variable-name")
        Map<String, String> containerEnv();

        /**
         * Whether to keep Dev Service containers running *after a dev mode session or test suite execution*
         * to reuse them in the next dev mode session or test suite execution.
         *
         * Within a dev mode session or test suite execution,
         * Quarkus will always reuse Dev Services as long as their configuration
         * (username, password, environment, port bindings, ...) did not change.
         * This feature is specifically about keeping containers running
         * **when Quarkus is not running** to reuse them across runs.
         *
         * WARNING: This feature needs to be enabled explicitly in `testcontainers.properties`,
         * may require changes to how you configure data initialization in dev mode and tests,
         * and may leave containers running indefinitely, forcing you to stop and remove them manually.
         * See xref:elasticsearch-dev-services.adoc#reuse[this section of the documentation] for more information.
         *
         * This configuration property is set to `true` by default,
         * so it is mostly useful to *disable* reuse,
         * if you enabled it in `testcontainers.properties`
         * but only want to use it for some of your Quarkus applications.
         *
         * @asciidoclet
         */
        @WithDefault("true")
        boolean reuse();

        enum Distribution {
            ELASTIC,
            OPENSEARCH
        }
    }
}
