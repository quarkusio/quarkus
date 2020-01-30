package io.quarkus.mailer.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.mailer.MailTemplate;
import io.quarkus.mailer.ReactiveMailer;
import io.quarkus.qute.api.ResourcePath;
import io.quarkus.qute.api.VariantTemplate;

@Singleton
public class MailTemplateProducer {

    private static final Logger LOGGER = Logger.getLogger(MailTemplateProducer.class);

    @Inject
    ReactiveMailer mailer;

    @Any
    Instance<VariantTemplate> template;

    @Produces
    MailTemplate getDefault(InjectionPoint injectionPoint) {

        final String name;
        if (injectionPoint.getMember() instanceof Field) {
            // For "@Inject MailTemplate test" use "test"
            name = injectionPoint.getMember().getName();
        } else {
            AnnotatedParameter<?> parameter = (AnnotatedParameter<?>) injectionPoint.getAnnotated();
            if (parameter.getJavaParameter().isNamePresent()) {
                name = parameter.getJavaParameter().getName();
            } else {
                name = injectionPoint.getMember().getName();
                LOGGER.warnf("Parameter name not present - using the method name as the template name instead %s", name);
            }
        }

        return new MailTemplate() {
            @Override
            public MailTemplateInstance instance() {
                return new MailTemplateInstanceImpl(mailer, template.select(new ResourcePath.Literal(name)).get().instance());
            }

        };
    }

    @ResourcePath("ignored")
    @Produces
    MailTemplate get(InjectionPoint injectionPoint) {
        ResourcePath path = null;
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(ResourcePath.class)) {
                path = (ResourcePath) qualifier;
                break;
            }
        }
        if (path == null || path.value().isEmpty()) {
            throw new IllegalStateException("No template reource path specified");
        }
        final String name = path.value();
        return new MailTemplate() {
            @Override
            public MailTemplateInstance instance() {
                return new MailTemplateInstanceImpl(mailer, template.select(new ResourcePath.Literal(name)).get().instance());
            }
        };
    }
}
