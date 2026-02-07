package io.quarkus.test.junit.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;
import org.junit.jupiter.api.Nested;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.test.common.QuarkusTestResource;
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
 * By default, QuarkusTests not using any profile come first, then QuarkusMainTests, then QuarkusIntegrationTests, then all
 * other
 * non-Quarkus tests (e.g. plain unit tests).<br/>
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
 * That order will also generally be maintained across groups, with each group running according to the position of its earliest
 * entry.
 * <p/>
 * Limitations:
 * <ul>
 * <li>Only JUnit5 test classes are subject to ordering, e.g. ArchUnit test classes are not passed to this orderer.</li>
 * <li>This orderer does not handle {@link Nested} test classes.</li>
 * </ul>
 */
public class QuarkusTestProfileAwareClassOrderer implements ClassOrderer {

    protected static final String DEFAULT_ORDER_PREFIX_QUARKUS_TEST = "20_";
    protected static final String DEFAULT_ORDER_PREFIX_QUARKUS_MAIN_TEST = "30_";
    protected static final String DEFAULT_ORDER_PREFIX_QUARKUS_INTEGRATION_TEST = "40_";
    protected static final String DEFAULT_ORDER_PREFIX_NON_QUARKUS_TEST = "60_";

    static final String CFGKEY_ORDER_PREFIX_QUARKUS_TEST = "junit.quarkus.orderer.prefix.quarkus-test";

    static final String CFGKEY_ORDER_PREFIX_QUARKUS_INTEGRATION_TEST = "junit.quarkus.orderer.prefix.quarkus-integration-test";

    static final String CFGKEY_ORDER_PREFIX_QUARKUS_MAIN_TEST = "junit.quarkus.orderer.prefix.quarkus-main-test";

    static final String CFGKEY_ORDER_PREFIX_NON_QUARKUS_TEST = "junit.quarkus.orderer.prefix.non-quarkus-test";

    static final String CFGKEY_SECONDARY_ORDERER = "junit.quarkus.orderer.secondary-orderer";

    private final String prefixQuarkusTest;
    private final String prefixQuarkusMainTest;
    private final String prefixQuarkusIntegrationTest;
    private final String prefixNonQuarkusTest;
    private final Optional<String> secondaryOrderer;

    public QuarkusTestProfileAwareClassOrderer() {
        Config config = ConfigProvider.getConfig();
        this.prefixQuarkusTest = config.getOptionalValue(CFGKEY_ORDER_PREFIX_QUARKUS_TEST, String.class)
                .orElse(DEFAULT_ORDER_PREFIX_QUARKUS_TEST);
        this.prefixQuarkusMainTest = config
                .getOptionalValue(CFGKEY_ORDER_PREFIX_QUARKUS_MAIN_TEST, String.class)
                .orElse(DEFAULT_ORDER_PREFIX_QUARKUS_MAIN_TEST);
        this.prefixQuarkusIntegrationTest = config.getOptionalValue(CFGKEY_ORDER_PREFIX_QUARKUS_INTEGRATION_TEST, String.class)
                .orElse(DEFAULT_ORDER_PREFIX_QUARKUS_INTEGRATION_TEST);
        this.prefixNonQuarkusTest = config.getOptionalValue(CFGKEY_ORDER_PREFIX_NON_QUARKUS_TEST, String.class)
                .orElse(DEFAULT_ORDER_PREFIX_NON_QUARKUS_TEST);
        this.secondaryOrderer = config.getOptionalValue(CFGKEY_SECONDARY_ORDERER, String.class);
    }

    QuarkusTestProfileAwareClassOrderer(
            final String prefixQuarkusTest,
            final String prefixQuarkusMainTest,
            final String prefixQuarkusIntegrationTest,
            final String prefixNonQuarkusTest,
            final Optional<String> secondaryOrderer) {
        this.prefixQuarkusTest = prefixQuarkusTest;
        this.prefixQuarkusMainTest = prefixQuarkusMainTest;
        this.prefixQuarkusIntegrationTest = prefixQuarkusIntegrationTest;
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

        // make a map recording the positions of each class descriptor after the initial sort
        Map<ClassDescriptor, Integer> firstPassIndex = new HashMap<>();
        for (int i = 0; i < classDescriptors.size(); i++) {
            firstPassIndex.put(classDescriptors.get(i), i);
        }

        // second pass: group by classloaders, and then assign an order to each classloader, based on the minimum secondary order within that group
        // Why group by classloader? The heavy lifting of understanding profiles and resources has been done elsewhere for Quarkus tests
        Map<ClassLoader, Integer> classLoaderOrder = classDescriptors.stream()
                .collect(Collectors.groupingBy(
                        cd -> cd.getTestClass().getClassLoader(),
                        Collectors.collectingAndThen(
                                Collectors.mapping(firstPassIndex::get,
                                        Collectors.minBy(Integer::compare)),
                                opt -> opt.orElse(Integer.MAX_VALUE))));

        // third pass: apply the actual Quarkus aware ordering logic, using the first and second pass indices as order key suffixes
        classDescriptors.sort(
                Comparator
                        .comparing((ClassDescriptor cd) -> getTypePrefix(cd))
                        .thenComparing(cd -> classLoaderOrder.get(cd.getTestClass().getClassLoader()))
                        .thenComparing(firstPassIndex::get));
    }

    private String getTypePrefix(ClassDescriptor classDescriptor) {
        // There are lots of ways something can be declared as a Quarkus test, and only some of them are annotations
        // Furthermore, isAnnotated only works for QuarkusTests if we reload the annotation class with the class's classloader
        // But we do know any QuarkusTest, and only QuarkusTests, will be loaded with the QuarkusClassLoader
        if (classDescriptor.getTestClass().getClassLoader() instanceof QuarkusClassLoader) {
            return prefixQuarkusTest;
        } else if (classDescriptor.isAnnotated(QuarkusIntegrationTest.class)) {
            return prefixQuarkusIntegrationTest;
        } else {
            if (classDescriptor.isAnnotated(QuarkusMainTest.class)) {
                return prefixQuarkusMainTest;
            } else {
                return prefixNonQuarkusTest;
            }
        }

    }
}
