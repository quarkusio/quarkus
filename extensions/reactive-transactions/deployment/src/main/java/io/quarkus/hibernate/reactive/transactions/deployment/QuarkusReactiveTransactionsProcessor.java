package io.quarkus.hibernate.reactive.transactions.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.reactive.transaction.TransactionalInterceptorMandatory;
import io.quarkus.reactive.transaction.TransactionalInterceptorNever;
import io.quarkus.reactive.transaction.TransactionalInterceptorNotSupported;
import io.quarkus.reactive.transaction.TransactionalInterceptorRequired;
import io.quarkus.reactive.transaction.TransactionalInterceptorRequiresNew;
import io.quarkus.reactive.transaction.TransactionalInterceptorSupports;

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
