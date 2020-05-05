package io.quarkus.liquibase.runtime.graal;

import java.util.List;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * The liquibase service locator substitute replaces liquibase classpath scanner method
 * {@link liquibase.servicelocator.ServiceLocator#findClasses(Class)} with a custom implementation
 * {@link LiquibaseServiceLoader#findClassesImpl(Class)}.
 */
@TargetClass(className = "liquibase.servicelocator.ServiceLocator")
final class SubstituteServiceLocator {

    @Substitute
    private List<Class<?>> findClassesImpl(Class<?> requiredInterface) {
        return LiquibaseServiceLoader.findClassesImpl(requiredInterface);
    }

}
