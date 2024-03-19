package io.quarkus.hibernate.search.orm.elasticsearch.runtime.graal;

import org.hibernate.search.util.common.jar.spi.JandexBehavior;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Disallow the use of Jandex so that relevant code can be DCEd
 * (otherwise native compilation would fail as Jandex is not available at runtime).
 */
@TargetClass(className = "org.hibernate.search.util.common.jar.spi.JandexBehavior")
final class Substitute_JandexBehavior {

    @Substitute
    public static void doWithJandex(JandexBehavior.JandexOperation operation) {
        throw new IllegalStateException("Jandex should not be used at runtime.");
    }

}
