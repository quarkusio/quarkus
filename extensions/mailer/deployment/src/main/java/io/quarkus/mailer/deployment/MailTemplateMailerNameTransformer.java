package io.quarkus.mailer.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.DotName;

public class MailTemplateMailerNameTransformer implements AnnotationTransformation {

    @Override
    public boolean supports(Kind kind) {
        return Kind.FIELD == kind || Kind.METHOD_PARAMETER == kind;
    }

    @Override
    public void apply(TransformationContext context) {
        DotName type;

        if (context.declaration().kind() == Kind.FIELD) {
            type = context.declaration().asField().type().name();
        } else {
            type = context.declaration().asMethodParameter().type().name();
        }

        if (!MailerProcessor.MAIL_TEMPLATE.equals(type)) {
            return;
        }

        AnnotationInstance mailerName = context.declaration().annotation(MailerProcessor.MAILER_NAME);
        if (mailerName == null) {
            return;
        }

        context.remove(ai -> MailerProcessor.MAILER_NAME.equals(ai.name()));
        context.add(AnnotationInstance.builder(MailerProcessor.MAIL_TEMPLATE_MAILER_NAME)
                .add(mailerName.value())
                .build());
    }

}
