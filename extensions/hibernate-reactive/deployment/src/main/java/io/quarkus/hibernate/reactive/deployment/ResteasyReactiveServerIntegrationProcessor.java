package io.quarkus.hibernate.reactive.deployment;

import jakarta.persistence.PersistenceException;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.resteasy.reactive.server.spi.UnwrappedExceptionBuildItem;

@BuildSteps(onlyIf = HibernateReactiveEnabled.class)
public class ResteasyReactiveServerIntegrationProcessor {

    @BuildStep
    public UnwrappedExceptionBuildItem unwrappedExceptions() {
        return new UnwrappedExceptionBuildItem(PersistenceException.class);
    }
}
