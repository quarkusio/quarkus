package io.quarkus.elasticsearch.restclient.lowlevel.runtime;

import java.util.Map;

import io.quarkus.elasticsearch.restclient.common.runtime.ElasticsearchClientBeanUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.elasticsearch")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ElasticsearchClientsRuntimeConfig {

    /**
     * Configuration for additional, named Elasticsearch clients.
     */
    @ConfigDocMapKey("client-name")
    @WithParentName
    @WithDefaults
    @WithUnnamedKey(ElasticsearchClientBeanUtil.DEFAULT_ELASTICSEARCH_CLIENT_NAME)
    Map<String, ElasticsearchClientRuntimeConfig> clients();

}
