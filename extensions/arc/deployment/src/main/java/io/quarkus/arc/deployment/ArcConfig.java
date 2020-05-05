package io.quarkus.arc.deployment;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_TIME;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
     * <li>If set to {@code all} (or {@code true}) the container will attempt to remove all unused beans.</li>
     * <li>If set to {@code none} (or {@code false}) no beans will ever be removed even if they are unused (according to the
     * criteria set out
     * below)</li>
     * <li>If set to {@code fwk}, then all unused beans will be removed, except the unused beans whose classes are declared in
     * the
     * application code</li>
     * </ul>
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
    @ConfigItem(defaultValue = "all")
    public String removeUnusedBeans;

    /**
     * If set to true {@code @Inject} is automatically added to all non-static fields that are annotated with
     * one of the annotations defined by {@link AutoInjectAnnotationBuildItem}.
     */
    @ConfigItem(defaultValue = "true")
    public boolean autoInjectFields;

    /**
     * If set to true, the bytecode of unproxyable beans will be transformed. This ensures that a proxy/subclass
     * can be created properly. If the value is set to false, then an exception is thrown at build time indicating that a
     * subclass/proxy could not be created.
     */
    @ConfigItem(defaultValue = "true")
    public boolean transformUnproxyableClasses;

    /**
     * The default naming strategy for {@link ConfigProperties.NamingStrategy}. The allowed values are determined
     * by that enum
     */
    @ConfigItem(defaultValue = "kebab-case")
    public ConfigProperties.NamingStrategy configPropertiesDefaultNamingStrategy;

    /**
     * The list of selected alternatives for an application.
     * <p>
     * An element value can be:
     * <ul>
     * <li>a fully qualified class name, i.e. {@code org.acme.Foo}</li>
     * <li>a simple class name as defined by {@link Class#getSimpleName()}, i.e. {@code Foo}</li>
     * <li>a package name with suffix {@code .*}, i.e. {@code org.acme.*}, matches a package</li>
     * <li>a package name with suffix {@code .**}, i.e. {@code org.acme.**}, matches a package that starts with the value</li>
     * </ul>
     * Each element value is used to match an alternative bean class, an alternative stereotype annotation type or a bean class
     * that declares an alternative producer. If any value matches then the priority of {@link Integer#MAX_VALUE} is used for
     * the relevant bean. The priority declared via {@link javax.annotation.Priority} or
     * {@link io.quarkus.arc.AlternativePriority} is overriden.
     */
    @ConfigItem
    public Optional<List<String>> selectedAlternatives;

    /**
     * If set to true then {@code javax.enterprise.inject.Produces} is automatically added to all methods that are
     * annotated with a scope annotation, a stereotype or a qualifier, and are not annotated with {@code Inject} or
     * {@code Produces}, and no parameter is annotated with {@code Disposes}, {@code Observes} or {@code ObservesAsync}.
     */
    @ConfigItem(defaultValue = "true")
    public boolean autoProducerMethods;

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
