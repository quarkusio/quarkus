package io.quarkus.cache.deployment.exception;

import org.jboss.jandex.ClassInfo;

public class KeyGeneratorConstructorException extends RuntimeException {

    private ClassInfo classInfo;

    public KeyGeneratorConstructorException(ClassInfo classInfo) {
        super("No default constructor found in cache key generator [class=" + classInfo.name() + "]");
        this.classInfo = classInfo;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }
}
