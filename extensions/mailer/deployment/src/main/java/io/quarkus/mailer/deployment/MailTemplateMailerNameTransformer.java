package io.quarkus.mailer.deployment;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationTransformation;

public class MailTemplateMailerNameTransformer implements AnnotationTransformation {

    @Override
    public boolean supports(Kind kind) {
        // Arc's annotation store uses compatible mode, which ignores transformations
        // whose declaration is a method parameter. Parameter annotations are visited
        // on the method instead.
        return Kind.FIELD == kind || Kind.METHOD == kind || Kind.METHOD_PARAMETER == kind;
    }

    @Override
    public void apply(TransformationContext context) {
        List<AnnotationInstance> mailerNames = new ArrayList<>();
        for (AnnotationInstance annotation : context.annotations()) {
            if (isMailTemplateMailerName(annotation)) {
                mailerNames.add(annotation);
            }
        }
        if (mailerNames.isEmpty()) {
            return;
        }

        context.remove(MailTemplateMailerNameTransformer::isMailTemplateMailerName);
        for (AnnotationInstance mailerName : mailerNames) {
            context.add(AnnotationInstance.builder(MailerProcessor.MAIL_TEMPLATE_MAILER_NAME)
                    .add(mailerName.value())
                    .buildWithTarget(mailerName.target()));
        }
    }

    private static boolean isMailTemplateMailerName(AnnotationInstance annotation) {
        if (annotation.value() == null || !MailerProcessor.MAILER_NAME.equals(annotation.name())) {
            return false;
        }
        AnnotationTarget target = annotation.target();
        if (target == null) {
            return false;
        }
        if (target.kind() == Kind.FIELD) {
            return MailerProcessor.MAIL_TEMPLATE.equals(target.asField().type().name());
        }
        if (target.kind() == Kind.METHOD_PARAMETER) {
            return MailerProcessor.MAIL_TEMPLATE.equals(target.asMethodParameter().type().name());
        }
        return false;
    }

}
