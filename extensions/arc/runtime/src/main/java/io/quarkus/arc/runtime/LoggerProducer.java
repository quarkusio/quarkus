package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.log.LoggerName;

@Singleton
public class LoggerProducer {

    private final ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<>();

    @Dependent
    @Produces
    @DefaultBean
    Logger getSimpleLogger(InjectionPoint injectionPoint) {
        return loggers.computeIfAbsent(injectionPoint.getMember().getDeclaringClass().getName(), Logger::getLogger);
    }

    @LoggerName("")
    @Dependent
    @Produces
    @DefaultBean
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
