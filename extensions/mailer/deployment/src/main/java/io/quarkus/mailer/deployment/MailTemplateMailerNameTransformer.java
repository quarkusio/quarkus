package io.quarkus.mailer.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.AnnotationsTransformer;

public class MailTemplateMailerNameTransformer implements AnnotationsTransformer {

    @Override
    public boolean appliesTo(Kind kind) {
        return Kind.FIELD == kind || Kind.METHOD_PARAMETER == kind;
    }

    @Override
    public void transform(TransformationContext transformationContext) {
        DotName type;

        if (transformationContext.getTarget().kind() == Kind.FIELD) {
            type = transformationContext.getTarget().asField().type().name();
        } else {
            type = transformationContext.getTarget().asMethodParameter().type().name();
        }

        if (!MailerProcessor.MAIL_TEMPLATE.equals(type)) {
            return;
        }

        AnnotationInstance mailerName = transformationContext.getTarget().annotation(MailerProcessor.MAILER_NAME);
        if (mailerName == null) {
            return;
        }

        transformationContext.transform().remove(ai -> MailerProcessor.MAILER_NAME.equals(ai.name()))
                .add(MailerProcessor.MAIL_TEMPLATE_MAILER_NAME, mailerName.value()).done();
    }

}
