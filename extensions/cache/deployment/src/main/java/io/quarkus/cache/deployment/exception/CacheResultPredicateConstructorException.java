package io.quarkus.cache.deployment.exception;

import org.jboss.jandex.ClassInfo;

public class CacheResultPredicateConstructorException extends RuntimeException {

    private final ClassInfo classInfo;

    public CacheResultPredicateConstructorException(ClassInfo classInfo) {
        super("No default constructor found in cache result predicate [class=" + classInfo.name() + "]");
        this.classInfo = classInfo;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }
}
