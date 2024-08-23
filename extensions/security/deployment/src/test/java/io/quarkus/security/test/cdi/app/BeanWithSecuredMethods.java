package io.quarkus.security.test.cdi.app;

import java.util.concurrent.CompletionStage;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import io.smallrye.mutiny.Uni;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@ApplicationScoped
@Named(BeanWithSecuredMethods.NAME)
public class BeanWithSecuredMethods {
    public static final String NAME = "super-bean";

    @DenyAll
    public String forbidden() {
        return "shouldBeDenied";
    }

    @RolesAllowed("admin")
    public String securedMethod() {
        return "accessibleForAdminOnly";
    }

    @RolesAllowed("admin")
    public Uni<String> securedMethodUni() {
        return Uni.createFrom().item("accessibleForAdminOnly");
    }

    @RolesAllowed("admin")
    public CompletionStage<String> securedMethodCompletionStage() {
        return Uni.createFrom().item("accessibleForAdminOnly").subscribeAsCompletionStage();
    }

    @RolesAllowed("admin")
    public CompletionStage<String> securedMethodCompletionStageException() {
        throw new TestException();
    }

    public String unsecuredMethod() {
        return "accessibleForAll";
    }
}
