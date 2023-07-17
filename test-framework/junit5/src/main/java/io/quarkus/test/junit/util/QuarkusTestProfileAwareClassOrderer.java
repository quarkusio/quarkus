package io.quarkus.test.junit.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;
import org.junit.jupiter.api.Nested;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.QuarkusMainTest;

/**
 * A {@link ClassOrderer} that orders {@link QuarkusTest}, {@link QuarkusIntegrationTest} and {@link QuarkusMainTest} classes
 * for minimum Quarkus
 * restarts by grouping them by their {@link TestProfile} and {@link QuarkusTestResource} annotation(s).
 * <p/>
 * By default, Quarkus*Tests not using any profile come first, then classes using a profile (in groups) and then all other
 * non-Quarkus tests (e.g. plain unit tests).<br/>
 * Quarkus*Tests with {@linkplain QuarkusTestResource#restrictToAnnotatedClass() restricted} {@code QuarkusTestResource} come
 * after tests with profiles and Quarkus*Tests with only unrestricted resources are handled like tests without a profile (come
 * first).
 * <p/>
 * Internally, ordering is based on prefixes that are prepended to a secondary order suffix (by default the fully qualified
 * name of the respective test class), with the fully qualified class name of the
 * {@link io.quarkus.test.junit.QuarkusTestProfile QuarkusTestProfile} as an infix (if present).
 * The default prefixes are defined by {@code DEFAULT_ORDER_PREFIX_*} and can be overridden in {@code junit-platform.properties}
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
    protected static final String DEFAULT_ORDER_PREFIX_QUARKUS_TEST_WITH_PROFILE = "40_";
    protected static final String DEFAULT_ORDER_PREFIX_QUARKUS_TEST_WITH_RESTRICTED_RES = "45_";
    protected static final String DEFAULT_ORDER_PREFIX_NON_QUARKUS_TEST = "60_";

    static final String CFGKEY_ORDER_PREFIX_QUARKUS_TEST = "junit.quarkus.orderer.prefix.quarkus-test";

    static final String CFGKEY_ORDER_PREFIX_QUARKUS_TEST_WITH_PROFILE = "junit.quarkus.orderer.prefix.quarkus-test-with-profile";

    static final String CFGKEY_ORDER_PREFIX_QUARKUS_TEST_WITH_RESTRICTED_RES = "junit.quarkus.orderer.prefix.quarkus-test-with-restricted-resource";

    static final String CFGKEY_ORDER_PREFIX_NON_QUARKUS_TEST = "junit.quarkus.orderer.prefix.non-quarkus-test";

    static final String CFGKEY_SECONDARY_ORDERER = "junit.quarkus.orderer.secondary-orderer";

    @Override
    public void orderClasses(ClassOrdererContext context) {
        // don't do anything if there is just one test class or the current order request is for @Nested tests
        if (context.getClassDescriptors().size() <= 1 || context.getClassDescriptors().get(0).isAnnotated(Nested.class)) {
            return;
        }
        var prefixQuarkusTest = getConfigParam(
                CFGKEY_ORDER_PREFIX_QUARKUS_TEST,
                DEFAULT_ORDER_PREFIX_QUARKUS_TEST,
                context);
        var prefixQuarkusTestWithProfile = getConfigParam(
                CFGKEY_ORDER_PREFIX_QUARKUS_TEST_WITH_PROFILE,
                DEFAULT_ORDER_PREFIX_QUARKUS_TEST_WITH_PROFILE,
                context);
        var prefixQuarkusTestWithRestrictedResource = getConfigParam(
                CFGKEY_ORDER_PREFIX_QUARKUS_TEST_WITH_RESTRICTED_RES,
                DEFAULT_ORDER_PREFIX_QUARKUS_TEST_WITH_RESTRICTED_RES,
                context);
        var prefixNonQuarkusTest = getConfigParam(
                CFGKEY_ORDER_PREFIX_NON_QUARKUS_TEST,
                DEFAULT_ORDER_PREFIX_NON_QUARKUS_TEST,
                context);

        // first pass: run secondary orderer first (!), which is easier than running it per "grouping"
        buildSecondaryOrderer(context).orderClasses(context);
        var classDecriptors = context.getClassDescriptors();
        var firstPassIndexMap = IntStream.range(0, classDecriptors.size()).boxed()
                .collect(Collectors.toMap(classDecriptors::get, i -> String.format("%06d", i)));

        // second pass: apply the actual Quarkus aware ordering logic, using the first pass indices as order key suffixes
        classDecriptors.sort(Comparator.comparing(classDescriptor -> {
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
                            var prefix = hasRestrictedResource(classDescriptor)
                                    ? prefixQuarkusTestWithRestrictedResource
                                    : prefixQuarkusTest;
                            return prefix + secondaryOrderSuffix;
                        });
            }
            return prefixNonQuarkusTest + secondaryOrderSuffix;
        }));
    }

    private String getConfigParam(String key, String fallbackValue, ClassOrdererContext context) {
        return context.getConfigurationParameter(key).orElse(fallbackValue);
    }

    private ClassOrderer buildSecondaryOrderer(ClassOrdererContext context) {
        return Optional.ofNullable(getConfigParam(CFGKEY_SECONDARY_ORDERER, null, context))
                .map(fqcn -> {
                    try {
                        return (ClassOrderer) Class.forName(fqcn).getDeclaredConstructor().newInstance();
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalArgumentException("Failed to instantiate " + fqcn, e);
                    }
                })
                .orElseGet(ClassOrderer.ClassName::new);
    }

    private boolean hasRestrictedResource(ClassDescriptor classDescriptor) {
        return classDescriptor.findRepeatableAnnotations(QuarkusTestResource.class).stream()
                .anyMatch(res -> res.restrictToAnnotatedClass() || isMetaTestResource(res, classDescriptor));
    }

    private boolean isMetaTestResource(QuarkusTestResource resource, ClassDescriptor classDescriptor) {
        return Arrays.stream(classDescriptor.getTestClass().getAnnotationsByType(QuarkusTestResource.class))
                .map(QuarkusTestResource::value)
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
