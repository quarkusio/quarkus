package io.quarkus.test.common;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration for search engine container images used in Quarkus tests.
 */
@ConfigMapping(prefix = "quarkus.container.image.search")
public interface SearchImagesConfig {


    @WithName("registry")
    @WithDefault("docker.io")
    String registry();


    @WithName("elasticsearch")
    @WithDefault("elastic/elasticsearch:${elasticsearch-server.version}")
    String elasticsearchImage();


    @WithName("logstash")
    @WithDefault("elastic/logstash:${elasticsearch-server.version}")
    String logstashImage();


    @WithName("kibana")
    @WithDefault("elastic/kibana:${elasticsearch-server.version}")
    String kibanaImage();


    @WithName("opensearch")
    @WithDefault("opensearchproject/opensearch:${opensearch-server.version}")
    String opensearchImage();


    default String getElasticsearchFullImage() {
        return registry() + "/" + elasticsearchImage();
    }


    default String getOpenSearchFullImage() {
        return registry() + "/" + opensearchImage();
    }


}