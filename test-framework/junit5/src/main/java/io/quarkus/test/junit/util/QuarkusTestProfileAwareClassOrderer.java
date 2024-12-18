package io.quarkus.test.junit.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;
import org.junit.jupiter.api.Nested;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.TestResourceScope;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.QuarkusMainTest;

/**
 * A {@link ClassOrderer} that orders {@link QuarkusTest}, {@link QuarkusIntegrationTest} and {@link QuarkusMainTest} classes
 * for minimum Quarkus
 * restarts by grouping them by their {@link TestProfile}, {@link WithTestResource}, and {@link QuarkusTestResource}
 * annotation(s).
 * <p/>
 * By default, Quarkus*Tests not using any profile come first, then classes using a profile (in groups) and then all other
 * non-Quarkus tests (e.g. plain unit tests).<br/>
 * Quarkus*Tests with {@linkplain WithTestResource#scope() matching resources} or
 * {@linkplain QuarkusTestResource#restrictToAnnotatedClass() restricted} {@code QuarkusTestResource} come
 * after tests with profiles and Quarkus*Tests with only unrestricted resources are handled like tests without a profile (come
 * first).
 * <p/>
 * Internally, ordering is based on prefixes that are prepended to a secondary order suffix (by default the fully qualified
 * name of the respective test class), with the fully qualified class name of the
 * {@link io.quarkus.test.junit.QuarkusTestProfile QuarkusTestProfile} as an infix (if present).
 * The default prefixes are defined by {@code DEFAULT_ORDER_PREFIX_*} and can be overridden in {@code application.properties}
 * via {@code CFGKEY_ORDER_PREFIX_*}, e.g. non-Quarkus tests can be run first (not last) by setting
 * {@link #CFGKEY_ORDER_PREFIX_NON_QUARKUS_TEST} to {@code 10_}.
 * <p/>
 * The secondary order suffix can be changed via {@value #CFGKEY_SECONDARY_ORDERER}, e.g. a value of
 * {@link org.junit.jupiter.api.ClassOrderer.Random org.junit.jupiter.api.ClassOrderer$Random} will order the test classes
 * within one group randomly instead by class name.
 * <p/>
 * {@link #getCustomOrderKey(ClassDescriptor, ClassOrdererContext)} can be overridden to provide a custom order number for a
 * given test class, e.g. based on {@link org.junit.jupiter.api.Tag} or something else.
 * <p/>
 * Limitations:
 * <ul>
 * <li>Only JUnit5 test classes are subject to ordering, e.g. ArchUnit test classes are not passed to this orderer.</li>
 * <li>This orderer does not handle {@link Nested} test classes.</li>
 * </ul>
 */
public class QuarkusTestProfileAwareClassOrderer implements ClassOrderer {

    protected static final String DEFAULT_ORDER_PREFIX_QUARKUS_TEST = "20_";
    protected static final String DEFAULT_ORDER_PREFIX_QUARKUS_TEST_WITH_MATCHING_RES = "30_";
    protected static final String DEFAULT_ORDER_PREFIX_QUARKUS_TEST_WITH_PROFILE = "40_";
    protected static final String DEFAULT_ORDER_PREFIX_QUARKUS_TEST_WITH_RESTRICTED_RES = "45_";
    protected static final String DEFAULT_ORDER_PREFIX_NON_QUARKUS_TEST = "60_";

    static final String CFGKEY_ORDER_PREFIX_QUARKUS_TEST = "junit.quarkus.orderer.prefix.quarkus-test";

    static final String CFGKEY_ORDER_PREFIX_QUARKUS_TEST_WITH_MATCHING_RES = "junit.quarkus.orderer.prefix.quarkus-test-with-matching-resource";

    static final String CFGKEY_ORDER_PREFIX_QUARKUS_TEST_WITH_PROFILE = "junit.quarkus.orderer.prefix.quarkus-test-with-profile";

    static final String CFGKEY_ORDER_PREFIX_QUARKUS_TEST_WITH_RESTRICTED_RES = "junit.quarkus.orderer.prefix.quarkus-test-with-restricted-resource";

    static final String CFGKEY_ORDER_PREFIX_NON_QUARKUS_TEST = "junit.quarkus.orderer.prefix.non-quarkus-test";

    static final String CFGKEY_SECONDARY_ORDERER = "junit.quarkus.orderer.secondary-orderer";

    private final String prefixQuarkusTest;
    private final String prefixQuarkusTestWithProfile;
    private final String prefixQuarkusTestWithRestrictedResource;
    private final String prefixQuarkusTestWithMatchingResource;
    private final String prefixNonQuarkusTest;
    private final Optional<String> secondaryOrderer;

    public QuarkusTestProfileAwareClassOrderer() {
        Config config = ConfigProvider.getConfig();
        this.prefixQuarkusTest = config.getOptionalValue(CFGKEY_ORDER_PREFIX_QUARKUS_TEST, String.class)
                .orElse(DEFAULT_ORDER_PREFIX_QUARKUS_TEST);
        this.prefixQuarkusTestWithMatchingResource = config
                .getOptionalValue(CFGKEY_ORDER_PREFIX_QUARKUS_TEST_WITH_MATCHING_RES, String.class)
                .orElse(DEFAULT_ORDER_PREFIX_QUARKUS_TEST_WITH_MATCHING_RES);
        this.prefixQuarkusTestWithProfile = config.getOptionalValue(CFGKEY_ORDER_PREFIX_QUARKUS_TEST_WITH_PROFILE, String.class)
                .orElse(DEFAULT_ORDER_PREFIX_QUARKUS_TEST_WITH_PROFILE);
        this.prefixQuarkusTestWithRestrictedResource = config
                .getOptionalValue(CFGKEY_ORDER_PREFIX_QUARKUS_TEST_WITH_RESTRICTED_RES, String.class)
                .orElse(DEFAULT_ORDER_PREFIX_QUARKUS_TEST_WITH_RESTRICTED_RES);
        this.prefixNonQuarkusTest = config.getOptionalValue(CFGKEY_ORDER_PREFIX_NON_QUARKUS_TEST, String.class)
                .orElse(DEFAULT_ORDER_PREFIX_NON_QUARKUS_TEST);
        this.secondaryOrderer = config.getOptionalValue(CFGKEY_SECONDARY_ORDERER, String.class);
    }

    QuarkusTestProfileAwareClassOrderer(
            final String prefixQuarkusTest,
            final String prefixQuarkusTestWithMatchingResource,
            final String prefixQuarkusTestWithProfile,
            final String prefixQuarkusTestWithRestrictedResource,
            final String prefixNonQuarkusTest,
            final Optional<String> secondaryOrderer) {
        this.prefixQuarkusTest = prefixQuarkusTest;
        this.prefixQuarkusTestWithMatchingResource = prefixQuarkusTestWithMatchingResource;
        this.prefixQuarkusTestWithProfile = prefixQuarkusTestWithProfile;
        this.prefixQuarkusTestWithRestrictedResource = prefixQuarkusTestWithRestrictedResource;
        this.prefixNonQuarkusTest = prefixNonQuarkusTest;
        this.secondaryOrderer = secondaryOrderer;
    }

    @Override
    public void orderClasses(ClassOrdererContext context) {
        // don't do anything if there is just one test class or the current order request is for @Nested tests
        if (context.getClassDescriptors()
                .size() <= 1 || context.getClassDescriptors()
                        .get(0)
                        .isAnnotated(Nested.class)) {
            return;
        }

        // In many cases (like QuarkusTest), the heavy lifting of understanding profiles and resources has been done elsewhere; we just need to group tests by classloader
        // However, for integration tests and main tests, profiles will not have been read
        long classloaderCount = context.getClassDescriptors()
                .stream()
                .map(d -> d.getTestClass()
                        .getClassLoader())
                .distinct()
                .count();

        // TODO this check probably isn't enough, because if there's a mix of QuarkusMain and other tests, we will have more than one classloader, but the others will still need sorting
        // TODO a safer check will be to assume anything loaded with a QuarkusClassLoader is pre-sorted, but to sort anything else.
        // TODO So sort by classloader, and then sort the pile which is in the system (or whatever) classloader
        if (classloaderCount > 1) {

            // If we sort first before applying the classloader sorting, the original order will be preserved within classloader groups
            secondaryOrderer
                    .map(fqcn -> {
                        try {
                            return (ClassOrderer) Class.forName(fqcn).getDeclaredConstructor().newInstance();
                        } catch (ReflectiveOperationException e) {
                            throw new IllegalArgumentException("Failed to instantiate " + fqcn, e);
                        }
                    })
                    .orElseGet(ClassName::new).orderClasses(context);

            context.getClassDescriptors().sort(Comparator.<ClassDescriptor, String> comparing(o -> o.getTestClass()
                    .getClassLoader()
                    .getName()));

        } else {
            orderByProfiles(context);
        }
    }

    private void orderByProfiles(ClassOrdererContext context) {

        // don't do anything if there is just one test class or the current order request is for @Nested tests
        if (context.getClassDescriptors()
                .size() <= 1 || context.getClassDescriptors()
                        .get(0)
                        .isAnnotated(Nested.class)) {
            return;
        }

        // first pass: run secondary orderer first (!), which is easier than running it per "grouping"
        secondaryOrderer
                .map(fqcn -> {
                    try {
                        return (ClassOrderer) Class.forName(fqcn).getDeclaredConstructor().newInstance();
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalArgumentException("Failed to instantiate " + fqcn, e);
                    }
                })
                .orElseGet(ClassName::new).orderClasses(context);

        var classDescriptors = context.getClassDescriptors();
        var firstPassIndexMap = IntStream.range(0, classDescriptors.size()).boxed()
                .collect(Collectors.toMap(classDescriptors::get, i -> String.format("%06d", i)));

        // second pass: apply the actual Quarkus aware ordering logic, using the first pass indices as order key suffixes
        classDescriptors.sort(Comparator.comparing(classDescriptor -> {
            var secondaryOrderSuffix = firstPassIndexMap.get(classDescriptor);
            Optional<String> customOrderKey = getCustomOrderKey(classDescriptor, context, secondaryOrderSuffix)
                    .or(() -> getCustomOrderKey(classDescriptor, context));
            if (customOrderKey.isPresent()) {
                return customOrderKey.get();
            }
            if (classDescriptor.isAnnotated(QuarkusTest.class)
                    || classDescriptor.isAnnotated(QuarkusIntegrationTest.class)
                    || classDescriptor.isAnnotated(QuarkusMainTest.class)) {
                return classDescriptor.findAnnotation(TestProfile.class)
                        .map(TestProfile::value)
                        .map(profileClass -> prefixQuarkusTestWithProfile + profileClass.getName() + "@" + secondaryOrderSuffix)
                        .orElseGet(() -> {
                            // TODO it should be possible to re-use the resource key here for (a) less code and (b) guaranteed consistency
                            // TODO we should probably also extend the key logic to a profile key ?
                            String prefix = prefixQuarkusTest;
                            String suffix = "";
                            TestResourceScope mostLimitedScope = mostLimitedScope(classDescriptor);
                            if (mostLimitedScope != null) {
                                if (mostLimitedScope == TestResourceScope.RESTRICTED_TO_CLASS) {
                                    prefix = prefixQuarkusTestWithRestrictedResource;
                                } else {
                                    prefix = prefixQuarkusTestWithMatchingResource;
                                    suffix = String.join(",",
                                            lifecycleManagerClassNamesForNonRestricted(classDescriptor));
                                }
                            }
                            return prefix + suffix + secondaryOrderSuffix;
                        });
            }
            return prefixNonQuarkusTest + secondaryOrderSuffix;
        }));
    }

    private TestResourceScope mostLimitedScope(ClassDescriptor classDescriptor) {
        TestResourceScope result = null;
        for (WithTestResource annotation : classDescriptor.findRepeatableAnnotations(WithTestResource.class)) {
            if (isMetaTestResource(annotation, classDescriptor)) {
                result = TestResourceScope.RESTRICTED_TO_CLASS;
            } else {
                TestResourceScope scope = annotation.scope();
                if ((result == null) || (scope.compareTo(result) < 0)) {
                    result = scope;
                }
            }
        }

        for (QuarkusTestResource annotation : classDescriptor.findRepeatableAnnotations(QuarkusTestResource.class)) {
            if (isMetaTestResource(annotation, classDescriptor) || annotation.restrictToAnnotatedClass()) {
                result = TestResourceScope.RESTRICTED_TO_CLASS;
            } else {
                result = TestResourceScope.GLOBAL;
            }
        }

        return result;
    }

    private Set<String> lifecycleManagerClassNamesForNonRestricted(ClassDescriptor classDescriptor) {
        Set<String> result = new HashSet<>();
        for (WithTestResource annotation : classDescriptor.findRepeatableAnnotations(WithTestResource.class)) {
            TestResourceScope scope = annotation.scope();
            if ((scope != TestResourceScope.RESTRICTED_TO_CLASS) && !isMetaTestResource(annotation, classDescriptor)) {
                result.add(annotation.value().getSimpleName());
            }
        }

        for (QuarkusTestResource annotation : classDescriptor.findRepeatableAnnotations(QuarkusTestResource.class)) {
            if (!annotation.restrictToAnnotatedClass() && !isMetaTestResource(annotation, classDescriptor)) {
                result.add(annotation.value().getSimpleName());
            }
        }

        return result;
    }

    @Deprecated(forRemoval = true)
    private boolean isMetaTestResource(QuarkusTestResource resource, ClassDescriptor classDescriptor) {
        return Arrays.stream(classDescriptor.getTestClass()
                .getAnnotationsByType(QuarkusTestResource.class))
                .map(QuarkusTestResource::value)
                .noneMatch(resource.value()::equals);
    }

    private boolean isMetaTestResource(WithTestResource resource, ClassDescriptor classDescriptor) {
        return Arrays.stream(classDescriptor.getTestClass()
                .getAnnotationsByType(WithTestResource.class))
                .map(WithTestResource::value)
                .noneMatch(resource.value()::equals);
    }

    /**
     * Template method that provides an optional custom order key for the given {@code classDescriptor}.
     *
     * @param classDescriptor the respective test class
     * @param context for config lookup
     * @return optional custom order key for the given test class
     * @deprecated use {@link #getCustomOrderKey(ClassDescriptor, ClassOrdererContext, String)} instead
     */
    @Deprecated(forRemoval = true, since = "2.7.0.CR1")
    protected Optional<String> getCustomOrderKey(ClassDescriptor classDescriptor, ClassOrdererContext context) {
        return Optional.empty();
    }

    /**
     * Template method that provides an optional custom order key for the given {@code classDescriptor}.
     *
     * @param classDescriptor the respective test class
     * @param context for config lookup
     * @param secondaryOrderSuffix the secondary order suffix that was calculated by the secondary orderer
     * @return optional custom order key for the given test class
     */
    protected Optional<String> getCustomOrderKey(ClassDescriptor classDescriptor, ClassOrdererContext context,
            String secondaryOrderSuffix) {
        return Optional.empty();
    }
}
