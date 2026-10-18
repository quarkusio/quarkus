package io.quarkus.hibernate.reactive.transactions.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.reactive.transaction.runtime.TransactionalInterceptorMandatory;
import io.quarkus.reactive.transaction.runtime.TransactionalInterceptorNever;
import io.quarkus.reactive.transaction.runtime.TransactionalInterceptorNotSupported;
import io.quarkus.reactive.transaction.runtime.TransactionalInterceptorRequired;
import io.quarkus.reactive.transaction.runtime.TransactionalInterceptorRequiresNew;
import io.quarkus.reactive.transaction.runtime.TransactionalInterceptorSupports;

public class QuarkusReactiveTransactionsProcessor {

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
}
