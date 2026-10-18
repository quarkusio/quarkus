package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.Messages;
import jakarta.enterprise.inject.build.compatible.spi.ObserverInfo;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.enterprise.lang.model.AnnotationTarget;

import org.jboss.logging.Logger;

class MessagesImpl implements Messages {
    private final SharedErrors errors;
    private final Logger log;

    MessagesImpl(SharedErrors errors, org.jboss.jandex.ClassInfo extensionClass) {
        this.errors = errors;
        this.log = Logger.getLogger(extensionClass.name().toString());
    }

    @Override
    public void info(String message) {
        log.info(message);
    }

    @Override
    public void info(String message, AnnotationTarget relatedTo) {
        log.info(message + " at " + relatedTo);
    }

    @Override
    public void info(String message, BeanInfo relatedTo) {
        log.info(message + " at " + relatedTo);
    }

    @Override
    public void info(String message, ObserverInfo relatedTo) {
        log.info(message + " at " + relatedTo);
    }

    @Override
    public void warn(String message) {
        log.warn(message);
    }

    @Override
    public void warn(String message, AnnotationTarget relatedTo) {
        log.warn(message + " at " + relatedTo);
    }

    @Override
    public void warn(String message, BeanInfo relatedTo) {
        log.warn(message + " at " + relatedTo);
    }

    @Override
    public void warn(String message, ObserverInfo relatedTo) {
        log.warn(message + " at " + relatedTo);
    }

    @Override
    public void error(String message) {
        log.error(message);
        errors.add(new DeploymentException(message));
    }

    @Override
    public void error(String message, AnnotationTarget relatedTo) {
        log.error(message + " at " + relatedTo);
        errors.add(new DeploymentException(message + " at " + relatedTo));
    }

    @Override
    public void error(String message, BeanInfo relatedTo) {
        log.error(message + " at " + relatedTo);
        errors.add(new DeploymentException(message + " at " + relatedTo));
    }

    @Override
    public void error(String message, ObserverInfo relatedTo) {
        log.error(message + " at " + relatedTo);
        errors.add(new DeploymentException(message + " at " + relatedTo));
    }

    @Override
    public void error(Exception exception) {
        log.error(exception.getMessage());
        errors.add(exception);
    }
}
