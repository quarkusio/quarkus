package io.quarkus.hibernate.orm.deployment;

import javax.persistence.PersistenceException;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.resteasy.reactive.server.spi.UnwrappedExceptionBuildItem;

@BuildSteps(onlyIf = HibernateOrmEnabled.class)
public class ResteasyReactiveServerIntegrationProcessor {

    @BuildStep
    public UnwrappedExceptionBuildItem unwrappedExceptions() {
        return new UnwrappedExceptionBuildItem(PersistenceException.class);
    }
}
