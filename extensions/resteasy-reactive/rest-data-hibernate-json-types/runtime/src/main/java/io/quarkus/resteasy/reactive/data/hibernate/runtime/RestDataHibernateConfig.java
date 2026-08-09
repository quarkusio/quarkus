package io.quarkus.resteasy.reactive.data.hibernate.runtime;

import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.rest.data")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface RestDataHibernateConfig {

    /**
     * Page request configuration.
     */
    PageConfig page();

    interface PageConfig {

        /**
         * The maximum page size accepted from a REST request. When not configured, the page size is not limited.
         */
        OptionalInt maxSize();
    }
}
