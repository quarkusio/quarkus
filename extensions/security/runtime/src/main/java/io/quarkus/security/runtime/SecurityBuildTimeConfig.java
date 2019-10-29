package io.quarkus.security.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@ConfigRoot(name = "security", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class SecurityBuildTimeConfig {
    /**
     * If set to true, access to all methods of beans that have any security annotations on other members will be denied by
     * default.
     * E.g. if enabled, in the following bean, <code>methodB</code> will be denied.
     * 
     * <pre>
     *   {@literal @}ApplicationScoped
     *   public class A {
     *      {@literal @}RolesAllowed("admin")
     *      public void methodA() {
     *          ...
     *      }
     *      public void methodB() {
     *          ...
     *      }
     *   }
     * </pre>
     */
    @ConfigItem(name = "deny-unannotated-members", defaultValue = "false")
    public boolean denyUnannotated;

}
