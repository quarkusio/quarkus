package io.quarkus.arc.deployment;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_TIME;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = BUILD_TIME)
public class ArcConfig {

    public static final Set<String> ALLOWED_REMOVE_UNUSED_BEANS_VALUES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("all", "true", "none", "false", "fwk", "framework")));

    /**
     * If set to all (or true) the container will attempt to remove all unused beans.
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
     * If set to none (or false) no beans will ever be removed even if they are unused (according to the criteria
     * set out above)
     *
     * If set to fwk, then all unused beans will be removed, except the unused beans whose classes are declared
     * in the application code
     * 
     * @see UnremovableBeanBuildItem
     */
    @ConfigItem(defaultValue = "all")
    public String removeUnusedBeans;

    /**
     * If set to true {@code @Inject} is automatically added to all non-static fields that are annotated with
     * one of the annotations defined by {@link AutoInjectAnnotationBuildItem}.
     */
    @ConfigItem(defaultValue = "true")
    public boolean autoInjectFields;

    public final boolean isRemoveUnusedBeansFieldValid() {
        return ALLOWED_REMOVE_UNUSED_BEANS_VALUES.contains(removeUnusedBeans.toLowerCase());
    }

    public final boolean shouldEnableBeanRemoval() {
        final String lowerCase = removeUnusedBeans.toLowerCase();
        return "all".equals(lowerCase) || "true".equals(lowerCase) || "fwk".equals(lowerCase) || "framework".equals(lowerCase);
    }

    public final boolean shouldOnlyKeepAppBeans() {
        final String lowerCase = removeUnusedBeans.toLowerCase();
        return "fwk".equals(lowerCase) || "framework".equals(lowerCase);
    }

}
