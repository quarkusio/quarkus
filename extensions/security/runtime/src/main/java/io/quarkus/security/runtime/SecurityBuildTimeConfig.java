package io.quarkus.security.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@ConfigMapping(prefix = "quarkus.security")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface SecurityBuildTimeConfig {
    /**
     * If set to true, access to all methods of beans that have any security annotations on other members will be denied by
     * default.
     * E.g. if enabled, in the following bean, <code>methodB</code> will be denied.
     *
     * <pre>
     *   &#064;ApplicationScoped
     *   public class A {
     *      &#064;RolesAllowed("admin")
     *      public void methodA() {
     *          ...
     *      }
     *      public void methodB() {
     *          ...
     *      }
     *   }
     * </pre>
     */
    @WithName("deny-unannotated-members")
    @WithDefault("false")
    boolean denyUnannotated();

}
