package io.quarkus.arc.runtime.test;

import java.util.Set;

import jakarta.inject.Singleton;

import io.quarkus.runtime.test.TestApplicationClassPredicate;

@Singleton
public class PreloadedTestApplicationClassPredicate implements TestApplicationClassPredicate {

    private volatile Set<String> applicationBeanClasses;

    @Override
    public boolean test(String name) {
        return applicationBeanClasses.contains(name);
    }

    public void setApplicationBeanClasses(Set<String> applicationBeanClasses) {
        this.applicationBeanClasses = applicationBeanClasses;
    }

}
