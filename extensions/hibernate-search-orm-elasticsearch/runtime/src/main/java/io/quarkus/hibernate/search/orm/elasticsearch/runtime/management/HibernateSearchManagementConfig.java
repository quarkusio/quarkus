package io.quarkus.hibernate.search.orm.elasticsearch.runtime.management;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.hibernate-search-orm.management")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface HibernateSearchManagementConfig {

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
