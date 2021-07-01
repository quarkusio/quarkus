package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Singleton;

import io.quarkus.arc.log.LoggerName;

@Singleton
public class JavaUtilLoggerProducer {

    private final ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<>();

    @Dependent
    @Produces
    Logger getSimpleLogger(InjectionPoint injectionPoint) {
        return loggers.computeIfAbsent(injectionPoint.getMember().getDeclaringClass().getName(), Logger::getLogger);
    }

    @LoggerName("")
    @Dependent
    @Produces
    Logger getLoggerWithCustomName(InjectionPoint injectionPoint) {
        String name = null;
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(LoggerName.class)) {
                name = ((LoggerName) qualifier).value();
            }
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalStateException("Unable to derive the logger name at " + injectionPoint);
        }
        return loggers.computeIfAbsent(name, Logger::getLogger);
    }

    @PreDestroy
    void destroy() {
        loggers.clear();
    }

}
