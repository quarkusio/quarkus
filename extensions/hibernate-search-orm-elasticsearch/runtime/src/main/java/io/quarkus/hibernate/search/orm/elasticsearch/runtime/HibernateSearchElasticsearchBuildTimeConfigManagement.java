package io.quarkus.hibernate.search.orm.elasticsearch.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface HibernateSearchElasticsearchBuildTimeConfigManagement {

    /**
     * Root path for reindexing endpoints.
     * This value will be resolved as a path relative to `${quarkus.management.root-path}`.
     *
     * @asciidoclet
     */
    @WithDefault("hibernate-search/")
    String rootPath();

    /**
     * If management interface is turned on the reindexing endpoints will be published under the management interface.
     * This property allows to enable this functionality by setting it to ``true`.
     *
     * @asciidoclet
     */
    @WithDefault("false")
    boolean enabled();

}
