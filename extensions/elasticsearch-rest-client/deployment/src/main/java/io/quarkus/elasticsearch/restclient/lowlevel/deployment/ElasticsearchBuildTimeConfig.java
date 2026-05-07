package io.quarkus.elasticsearch.restclient.lowlevel.deployment;

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
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ElasticsearchBuildTimeConfig {
    /**
     * Configuration for Elasticsearch clients.
     */
    @WithParentName
    @WithUnnamedKey(ElasticsearchClientBeanUtil.DEFAULT_ELASTICSEARCH_CLIENT_NAME)
    @WithDefaults
    @ConfigDocMapKey("elasticsearch-client-name")
    Map<String, ElasticsearchLowLevelClientBuildTimeConfig> clients();
}
