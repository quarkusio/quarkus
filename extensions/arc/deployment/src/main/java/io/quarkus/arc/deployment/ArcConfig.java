package io.quarkus.arc.deployment;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_TIME;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = BUILD_TIME)
public class ArcConfig {

    public static final Set<String> ALLOWED_REMOVE_UNUSED_BEANS_VALUES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("all", "true", "none", "false", "fwk", "framework")));

    /**
     * <ul>
     * <li>If set to `all` (or `true`) the container will attempt to remove all unused beans.</li>
     * <li>If set to none (or `false`) no beans will ever be removed even if they are unused (according to the criteria set out
     * below)</li>
     * <li>If set to `fwk`, then all unused beans will be removed, except the unused beans whose classes are declared in the
     * application code</li>
     * </ul>
     * <br>
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
     * </p>
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

    /**
     * If set to true, Arc will transform the bytecode of beans containing methods that need to be proxyable
     * but have been declared as final. The transformation is simply a matter of removing final.
     * This ensures that a proxy can be created properly.
     * If the value is set to false, then an exception is thrown at build time indicating
     * that a proxy could not be created because a method was final.
     */
    @ConfigItem(defaultValue = "true")
    public boolean removeFinalForProxyableMethods;

    /**
     * The default naming strategy for {@link ConfigProperties.NamingStrategy}. The allowed values are determined
     * by that enum
     */
    @ConfigItem(defaultValue = "kebab-case")
    public ConfigProperties.NamingStrategy configPropertiesDefaultNamingStrategy;

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
