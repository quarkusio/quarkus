package org.jboss.shamrock.arc.deployment;

import static org.jboss.shamrock.runtime.annotations.ConfigPhase.BUILD_TIME;

import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = BUILD_TIME)
public class ArcConfig {

    /**
     * If set to true the container will attempt to remove all unused beans.
     * <p>
     * An unused bean:
     * <ul>
     * <li>is not a built-in bean or interceptor,</li>
     * <li>is not eligible for injection to any injection point,</li>
     * <li>is not excluded by any extension,</li>
     * <li>does not have a name,</li>
     * <li>does not declare an observer,</li>
     * <li>does not declare any producer which is eligible for injection to any injection point,</li>
     * <li>is not directly eligible for injection into any {@link javax.enterprise.inject.Instance} injection point</li>
     * </ul>
     * 
     * @see UnremovableBeanBuildItem
     */
    @ConfigItem(defaultValue = "true")
    public boolean removeUnusedBeans;

}
