package io.quarkus.test.junit.util;

import java.util.Comparator;
import java.util.Optional;

import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * A {@link ClassOrderer} that orders {@link QuarkusTest} and {@link QuarkusIntegrationTest} classes for minimum Quarkus
 * restarts by grouping them by their {@link TestProfile}.
 * <p/>
 * By default, Quarkus*Tests not using any profile come first, then classes using a profile (in groups) and then all other
 * non-Quarkus tests (e.g. plain unit tests).
 * <p/>
 * Internally, ordering is based on three prefixes that are prepended to the fully qualified name of the respective class, with
 * the fully qualified class name of the {@link io.quarkus.test.junit.QuarkusTestProfile QuarkusTestProfile} as an infix (if
 * present).
 * The default prefixes are defined by {@code DEFAULT_ORDER_PREFIX_*} and can be overridden in {@code junit-platform.properties}
 * via {@code CFGKEY_ORDER_PREFIX_*}, e.g. non-Quarkus tests can be run first (not last) by setting
 * {@link #CFGKEY_ORDER_PREFIX_NON_QUARKUS_TEST} to {@code 10_}.
 * <p/>
 * {@link #getCustomOrderKey(ClassDescriptor, ClassOrdererContext)} can be overridden to provide a custom order number for a
 * given test class, e.g. based on {@link org.junit.jupiter.api.Tag}, class name or something else.
 * <p/>
 * Limitations:
 * <ul>
 * <li>This orderer does not (yet) consider {@linkplain io.quarkus.test.common.QuarkusTestResource#restrictToAnnotatedClass()
 * test resources that are restricted to the annotated class}.</li>
 * <li>Only JUnit5 test classes are subject to ordering, e.g. ArchUnit test classes are not passed to this orderer.</li>
 * </ul>
 */
public class QuarkusTestProfileAwareClassOrderer implements ClassOrderer {

    protected static final String DEFAULT_ORDER_PREFIX_QUARKUS_TEST = "20_";
    protected static final String DEFAULT_ORDER_PREFIX_QUARKUS_TEST_WITH_PROFILE = "40_";
    protected static final String DEFAULT_ORDER_PREFIX_NON_QUARKUS_TEST = "60_";

    static final String CFGKEY_ORDER_PREFIX_QUARKUS_TEST = "quarkus.test.orderer.prefix.quarkus-test";
    static final String CFGKEY_ORDER_PREFIX_QUARKUS_TEST_WITH_PROFILE = "quarkus.test.orderer.prefix.quarkus-test-with-profile";
    static final String CFGKEY_ORDER_PREFIX_NON_QUARKUS_TEST = "quarkus.test.orderer.prefix.non-quarkus-test";

    @Override
    public void orderClasses(ClassOrdererContext context) {
        if (context.getClassDescriptors().size() <= 1) {
            return;
        }
        var prefixQuarkusTest = context.getConfigurationParameter(CFGKEY_ORDER_PREFIX_QUARKUS_TEST)
                .orElse(DEFAULT_ORDER_PREFIX_QUARKUS_TEST);
        var prefixQuarkusTestWithProfile = context.getConfigurationParameter(CFGKEY_ORDER_PREFIX_QUARKUS_TEST_WITH_PROFILE)
                .orElse(DEFAULT_ORDER_PREFIX_QUARKUS_TEST_WITH_PROFILE);
        var prefixNonQuarkusTest = context.getConfigurationParameter(CFGKEY_ORDER_PREFIX_NON_QUARKUS_TEST)
                .orElse(DEFAULT_ORDER_PREFIX_NON_QUARKUS_TEST);

        context.getClassDescriptors().sort(Comparator.comparing(classDescriptor -> {
            Optional<String> customOrderKey = getCustomOrderKey(classDescriptor, context);
            if (customOrderKey.isPresent()) {
                return customOrderKey.get();
            }
            var testClassName = classDescriptor.getTestClass().getName();
            if (classDescriptor.isAnnotated(QuarkusTest.class)
                    || classDescriptor.isAnnotated(QuarkusIntegrationTest.class)) {
                return classDescriptor.findAnnotation(TestProfile.class)
                        .map(TestProfile::value)
                        .map(profileClass -> prefixQuarkusTestWithProfile + profileClass.getName() + "@" + testClassName)
                        .orElseGet(() -> prefixQuarkusTest + testClassName);
            }
            return prefixNonQuarkusTest + testClassName;
        }));
    }

    /**
     * Template method that provides an optional custom order key for the given {@code classDescriptor}.
     * 
     * @param classDescriptor the respective test class
     * @param context for config lookup
     * @return optional custom order key for the given test class
     */
    protected Optional<String> getCustomOrderKey(ClassDescriptor classDescriptor, ClassOrdererContext context) {
        return Optional.empty();
    }
}
