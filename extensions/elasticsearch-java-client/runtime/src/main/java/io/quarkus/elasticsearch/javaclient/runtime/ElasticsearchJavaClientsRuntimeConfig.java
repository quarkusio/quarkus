package io.quarkus.elasticsearch.javaclient.runtime;

import java.util.Map;

import io.quarkus.elasticsearch.restclient.common.runtime.ElasticsearchClientBeanUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.elasticsearch-java")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ElasticsearchJavaClientsRuntimeConfig {

    /**
     * Configuration for additional, named Elasticsearch Java clients.
     */
    @ConfigDocMapKey("client-name")
    @WithParentName
    @WithDefaults
    @WithUnnamedKey(ElasticsearchClientBeanUtil.DEFAULT_ELASTICSEARCH_CLIENT_NAME)
    Map<String, ElasticsearchJavaClientRuntimeConfig> clients();

}
