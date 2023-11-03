package io.quarkus.test.junit;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.TestInfo;

class TestInfoImpl implements TestInfo {

    private final String displayName;
    private final Set<String> tags;
    private final Optional<Class<?>> testClass;
    private final Optional<Method> testMethod;

    TestInfoImpl(String displayName, Set<String> tags, Optional<Class<?>> testClass, Optional<Method> testMethod) {
        this.displayName = displayName;
        this.tags = tags;
        this.testClass = testClass;
        this.testMethod = testMethod;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Set<String> getTags() {
        return tags;
    }

    @Override
    public Optional<Class<?>> getTestClass() {
        return testClass;
    }

    @Override
    public Optional<Method> getTestMethod() {
        return testMethod;
    }
}
