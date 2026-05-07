package io.quarkus.elasticsearch.restclient.common.deployment;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.builder.BuildException;
import io.quarkus.elasticsearch.restclient.common.deployment.ElasticsearchCommonBuildTimeConfig.ElasticsearchDevServicesBuildTimeConfig.Distribution;

class DevservicesElasticsearchBuildItemsConfiguration {
    Set<String> hostsConfigProperties;
    String version;
    Distribution distribution;

    DevservicesElasticsearchBuildItemsConfiguration(List<DevservicesElasticsearchBuildItem> buildItems)
            throws BuildException {
        hostsConfigProperties = new HashSet<>(buildItems.size());

        // check that all build items agree on the version and distribution to start
        for (DevservicesElasticsearchBuildItem buildItem : buildItems) {
            if (version == null) {
                version = buildItem.getVersion();
            } else if (buildItem.getVersion() != null && !version.equals(buildItem.getVersion())) {
                // safety guard but should never occur as only Hibernate Search ORM Elasticsearch configure the version
                throw new BuildException(
                        "Multiple extensions request different versions of Elasticsearch for Dev Services.",
                        Collections.emptyList());
            }

            if (distribution == null) {
                distribution = buildItem.getDistribution();
            } else if (buildItem.getDistribution() != null && !distribution.equals(buildItem.getDistribution())) {
                // safety guard but should never occur as only Hibernate Search ORM Elasticsearch configure the distribution
                throw new BuildException(
                        "Multiple extensions request different distributions of Elasticsearch for Dev Services.",
                        Collections.emptyList());
            }

            hostsConfigProperties.add(buildItem.getHostsConfigProperty());
        }
    }
}