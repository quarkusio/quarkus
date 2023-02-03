package io.quarkus.mailer.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.mailer.MailTemplate;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.runtime.LocationLiteral;

@Singleton
public class MailTemplateProducer {

    private static final Logger LOGGER = Logger.getLogger(MailTemplateProducer.class);

    @Inject
    MutinyMailerImpl mailer;

    @Any
    Instance<Template> template;

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
                return new MailTemplateInstanceImpl(mailer, template.select(new LocationLiteral(name)).get().instance());
            }

        };
    }

    @Location("ignored")
    @Produces
    MailTemplate get(InjectionPoint injectionPoint) {
        Location path = null;
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(Location.class)) {
                path = (Location) qualifier;
                break;
            }
        }
        if (path == null || path.value().isEmpty()) {
            throw new IllegalStateException("No template resource path specified");
        }
        final String name = path.value();
        return new MailTemplate() {
            @Override
            public MailTemplateInstance instance() {
                return new MailTemplateInstanceImpl(mailer, template.select(new LocationLiteral(name)).get().instance());
            }
        };
    }

    /**
     * Called by MailTemplateInstanceAdaptor
     */
    public static MailTemplate.MailTemplateInstance getMailTemplateInstance(TemplateInstance instance) {
        MutinyMailerImpl mailerImpl = Arc.container().instance(MutinyMailerImpl.class).get();
        return new MailTemplateInstanceImpl(mailerImpl, instance);
    }
}
