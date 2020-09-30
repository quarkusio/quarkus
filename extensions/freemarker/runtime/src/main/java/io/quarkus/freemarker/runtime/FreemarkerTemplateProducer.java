package io.quarkus.freemarker.runtime;

import java.io.IOException;
import java.lang.annotation.Annotation;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Singleton;

import freemarker.template.Configuration;
import freemarker.template.Template;

@Singleton
public class FreemarkerTemplateProducer {

    @Inject
    Configuration configuration;

    @Produces
    @TemplatePath("ignored")
    Template getTemplate(InjectionPoint injectionPoint) throws IOException {
        TemplatePath path = null;
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(TemplatePath.class)) {
                path = (TemplatePath) qualifier;
                break;
            }
        }
        if (path == null || path.value().isEmpty()) {
            throw new IllegalStateException("No template reource path specified");
        }
        return configuration.getTemplate(path.value());
    }
}
