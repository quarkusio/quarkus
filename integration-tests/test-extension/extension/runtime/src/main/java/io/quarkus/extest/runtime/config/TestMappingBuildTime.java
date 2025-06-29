package io.quarkus.extest.runtime.config;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.mapping.bt")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface TestMappingBuildTime {
    /**
     * A String value.
     */
    String value();

    /**
     * A nested Group.
     */
    Group group();

    /**
     * An Optional missing Group.
     */
    Optional<Group> missing();

    /**
     * An Optional present Group.
     */
    Optional<Group> present();

    /**
     * A List of Groups.
     */
    List<Group> groups();

    /**
     * An Optional List of Groups.
     */
    Optional<List<Group>> optionalGroups();

    /**
     * Deprecated
     *
     * @deprecated deprecated.
     */
    @Deprecated
    String deprecated();

    interface Group {
        /**
         * A Group value.
         */
        String value();
    }

    default void mustNotRequireDocs() {

    }
}
