package io.quarkus.arc.deployment;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_TIME;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.deployment.index.IndexDependencyConfig;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
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
     *
     * Quarkus performs the following transformations when this setting is enabled:
     * <ul>
     * <li>Remove 'final' modifier from classes and methods when a proxy is required.
     * <li>Create a no-args constructor if needed.
     * <li>Makes private no-args constructors package-private if necessary.
     * </ul
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
     * If set to true then {@code javax.enterprise.inject.Produces} is automatically added to all non-void methods that are
     * annotated with a scope annotation, a stereotype or a qualifier, and are not annotated with {@code Inject} or
     * {@code Produces}, and no parameter is annotated with {@code Disposes}, {@code Observes} or {@code ObservesAsync}.
     */
    @ConfigItem(defaultValue = "true")
    public boolean autoProducerMethods;

    /**
     * The list of types that should be excluded from discovery.
     * <p>
     * An element value can be:
     * <ul>
     * <li>a fully qualified class name, i.e. {@code org.acme.Foo}</li>
     * <li>a simple class name as defined by {@link Class#getSimpleName()}, i.e. {@code Foo}</li>
     * <li>a package name with suffix {@code .*}, i.e. {@code org.acme.*}, matches a package</li>
     * <li>a package name with suffix {@code .**}, i.e. {@code org.acme.**}, matches a package that starts with the value</li>
     * </ul>
     * If any element value matches a discovered type then the type is excluded from discovery, i.e. no beans and observer
     * methods are created from this type.
     */
    @ConfigItem
    public Optional<List<String>> excludeTypes;

    /**
     * List of types that should be considered unremovable regardless of whether they are directly used or not.
     * This is a configuration option equivalent to using {@link io.quarkus.arc.Unremovable} annotation.
     *
     * <p>
     * An element value can be:
     * <ul>
     * <li>a fully qualified class name, i.e. {@code org.acme.Foo}</li>
     * <li>a simple class name as defined by {@link Class#getSimpleName()}, i.e. {@code Foo}</li>
     * <li>a package name with suffix {@code .*}, i.e. {@code org.acme.*}, matches a package</li>
     * <li>a package name with suffix {@code .**}, i.e. {@code org.acme.**}, matches a package that starts with the value</li>
     * </ul>
     * If any element value matches a discovered bean, then such a bean is considered unremovable.
     *
     * @see {@link #removeUnusedBeans}
     * @see {@link io.quarkus.arc.Unremovable}
     */
    @ConfigItem
    public Optional<List<String>> unremovableTypes;

    /**
     * Artifacts that should be excluded from discovery.
     * <p>
     * These artifacts would be otherwise scanned for beans, i.e. they
     * contain a Jandex index or a beans.xml descriptor.
     */
    @ConfigItem
    @ConfigDocSection
    @ConfigDocMapKey("dependency-name")
    Map<String, IndexDependencyConfig> excludeDependency;

    /**
     * If set to true then the container attempts to detect "unused removed beans" false positives during programmatic lookup at
     * runtime. You can disable this feature to conserve some memory when running your application in production.
     *
     * @see ArcConfig#removeUnusedBeans
     */
    @ConfigItem(defaultValue = "true")
    public boolean detectUnusedFalsePositives;

    /**
     * If set to true then the container attempts to detect <i>wrong</i> usages of annotations and eventually fails the build to
     * prevent unexpected behavior of a Quarkus application.
     * <p>
     * A typical example is {@code @javax.ejb.Singleton} which is often confused with {@code @javax.inject.Singleton}. As a
     * result a component annotated with {@code @javax.ejb.Singleton} would be completely ignored. Another example is an inner
     * class annotated with a scope annotation - this component would be again completely ignored.
     */
    @ConfigItem(defaultValue = "true")
    public boolean detectWrongAnnotations;

    /**
     * Dev mode configuration.
     */
    @ConfigItem
    public ArcDevModeConfig devMode;

    /**
     * Test mode configuration.
     */
    @ConfigItem
    public ArcTestConfig test;

    /**
     * The list of packages that will not be checked for split package issues.
     * <p>
     * A package string representation can be:
     * <ul>
     * <li>a full name of the package, i.e. {@code org.acme.foo}</li>
     * <li>a package name with suffix {@code .*}, i.e. {@code org.acme.*}, which matches a package that starts with provided
     * value</li>
     */
    @ConfigItem
    public Optional<List<String>> ignoredSplitPackages;

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
