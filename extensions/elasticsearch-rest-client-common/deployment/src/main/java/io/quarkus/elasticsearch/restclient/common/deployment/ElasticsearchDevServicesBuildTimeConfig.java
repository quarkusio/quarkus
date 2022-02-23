package io.quarkus.elasticsearch.restclient.common.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "elasticsearch.devservices", phase = ConfigPhase.BUILD_TIME)
public class ElasticsearchDevServicesBuildTimeConfig {

    /**
     * If Dev Services for Elasticsearch has been explicitly enabled or disabled. Dev Services are generally enabled
     * by default, unless there is an existing configuration present. For Elasticsearch, Dev Services starts a server unless
     * {@code quarkus.elasticsearch.hosts} is set.
     */
    @ConfigItem
    public Optional<Boolean> enabled = Optional.empty();

    /**
     * Optional fixed port the dev service will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    @ConfigItem
    public Optional<Integer> port;

    /**
     * The Elasticsearch container image to use.
     * Defaults to the elasticsearch image provided by Elastic.
     */
    @ConfigItem(defaultValue = "docker.elastic.co/elasticsearch/elasticsearch:7.17.0")
    public String imageName;

    /**
     * The value for the ES_JAVA_OPTS env variable.
     * Defaults to setting the heap to 1GB.
     */
    @ConfigItem(defaultValue = "-Xmx1g")
    public String javaOpts;

    /**
     * Indicates if the Elasticsearch server managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services for Elasticsearch starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-elasticsearch} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @ConfigItem(defaultValue = "true")
    public boolean shared;

    /**
     * The value of the {@code quarkus-dev-service-elasticsearch} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for Elasticsearch looks for a container with the
     * {@code quarkus-dev-service-elasticsearch} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise it
     * starts a new container with the {@code quarkus-dev-service-elasticsearch} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared Elasticsearch servers.
     */
    @ConfigItem(defaultValue = "elasticsearch")
    public String serviceName;
}
