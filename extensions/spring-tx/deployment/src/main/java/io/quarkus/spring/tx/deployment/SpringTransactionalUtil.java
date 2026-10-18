package io.quarkus.spring.tx.deployment;

import org.jboss.jandex.AnnotationTarget;

final class SpringTransactionalUtil {

    private SpringTransactionalUtil() {
    }

    static String describeTarget(AnnotationTarget target) {
        if (target.kind() == AnnotationTarget.Kind.METHOD) {
            return "method '" + target.asMethod().name()
                    + "' of class '" + target.asMethod().declaringClass().name() + "'";
        } else if (target.kind() == AnnotationTarget.Kind.CLASS) {
            return "class '" + target.asClass().name() + "'";
        }
        return target.toString();
    }
}
