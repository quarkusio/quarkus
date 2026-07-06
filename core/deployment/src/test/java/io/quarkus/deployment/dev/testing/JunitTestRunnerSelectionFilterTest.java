package io.quarkus.deployment.dev.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor.Type;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.PostDiscoveryFilter;

class JunitTestRunnerSelectionFilterTest {

    private static final String NESTED_TEST_CLASS = "org.acme.QuarkusNestedTest$NestedClassTest";

    @Test
    void mavenClassSelectionIncludesNestedTestClass() throws Exception {
        PostDiscoveryFilter filter = mavenFilter("QuarkusNestedTest");

        assertThat(apply(filter, NESTED_TEST_CLASS, "nestedTest")).isTrue();
        assertThat(apply(filter, "org.acme.QuarkusNestedTestFoo", "otherTest")).isFalse();
    }

    @Test
    void mavenClassAndMethodSelectionIncludesNestedTestClass() throws Exception {
        PostDiscoveryFilter filter = mavenFilter("QuarkusNestedTest#nestedTest");

        assertThat(apply(filter, NESTED_TEST_CLASS, "nestedTest")).isTrue();
        assertThat(apply(filter, NESTED_TEST_CLASS, "otherTest")).isFalse();
    }

    @Test
    void gradleClassSelectionIncludesNestedTestClass() throws Exception {
        PostDiscoveryFilter filter = gradleFilter("QuarkusNestedTest");

        assertThat(apply(filter, NESTED_TEST_CLASS, "nestedTest")).isTrue();
        assertThat(apply(filter, "org.acme.QuarkusNestedTestFoo", "otherTest")).isFalse();
    }

    @Test
    void gradleClassAndMethodSelectionIncludesNestedTestClass() throws Exception {
        PostDiscoveryFilter filter = gradleFilter("QuarkusNestedTest.nestedTest");

        assertThat(apply(filter, NESTED_TEST_CLASS, "nestedTest")).isTrue();
        assertThat(apply(filter, NESTED_TEST_CLASS, "otherTest")).isFalse();
    }

    private static PostDiscoveryFilter mavenFilter(String selection) throws Exception {
        return filter("MavenSpecificSelectionFilter", selection);
    }

    private static PostDiscoveryFilter gradleFilter(String selection) throws Exception {
        return filter("GradleSpecificSelectionFilter", selection);
    }

    private static PostDiscoveryFilter filter(String filterName, String selection) throws Exception {
        Class<?> filterClass = Class.forName(JunitTestRunner.class.getName() + "$" + filterName);
        Constructor<?> constructor = filterClass.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        return (PostDiscoveryFilter) constructor.newInstance(selection);
    }

    private static boolean apply(PostDiscoveryFilter filter, String className, String methodName) {
        FilterResult result = filter.apply(new MethodTestDescriptor(className, methodName));
        return result.included();
    }

    private static final class MethodTestDescriptor extends AbstractTestDescriptor {

        private MethodTestDescriptor(String className, String methodName) {
            super(UniqueId.forEngine("test").append("method", className + "#" + methodName), methodName,
                    MethodSource.from(className, methodName));
        }

        @Override
        public Type getType() {
            return Type.TEST;
        }
    }
}
