package io.quarkus.hibernate.reactive.transactions.deployment;

import jakarta.transaction.Transactional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.reactive.transaction.TransactionalInterceptorMandatory;
import io.quarkus.reactive.transaction.TransactionalInterceptorNever;
import io.quarkus.reactive.transaction.TransactionalInterceptorNotSupported;
import io.quarkus.reactive.transaction.TransactionalInterceptorRequired;
import io.quarkus.reactive.transaction.TransactionalInterceptorRequiresNew;
import io.quarkus.reactive.transaction.TransactionalInterceptorSupports;
import io.smallrye.mutiny.Multi;

public class QuarkusReactiveTransactionsProcessor {

    private static final DotName TRANSACTIONAL = DotName.createSimple(Transactional.class.getName());
    private static final DotName MULTI = DotName.createSimple(Multi.class.getName());

    @BuildStep
    AdditionalBeanBuildItem produceItems() {
        return new AdditionalBeanBuildItem(
                TransactionalInterceptorMandatory.class,
                TransactionalInterceptorNever.class,
                TransactionalInterceptorNotSupported.class,
                TransactionalInterceptorRequired.class,
                TransactionalInterceptorRequiresNew.class,
                TransactionalInterceptorSupports.class);

    }

    @BuildStep
    void validateTransactionalMethods(ApplicationIndexBuildItem applicationIndex,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors) {

        IndexView index = applicationIndex.getIndex();

        // Find all methods and classes annotated with @Transactional
        for (AnnotationInstance annotation : index.getAnnotations(TRANSACTIONAL)) {
            if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                MethodInfo method = annotation.target().asMethod();
                validateMethodReturnType(method, validationErrors);
            }
        }
    }

    private void validateMethodReturnType(MethodInfo method,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors) {
        Type returnType = method.returnType();

        if (returnType.name().equals(MULTI)) {
            validationErrors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                    new IllegalStateException(String.format(
                            "@Transactional methods cannot return Multi. Method %s.%s() in class %s must return Uni instead.",
                            method.declaringClass().name(),
                            method.name(),
                            method.declaringClass().name()))));
        }
    }
}
