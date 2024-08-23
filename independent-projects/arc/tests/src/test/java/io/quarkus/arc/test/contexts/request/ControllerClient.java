package io.quarkus.arc.test.contexts.request;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

@ApplicationScoped
public class ControllerClient {

    @Inject
    Controller controller;

    @ActivateRequestContext
    String getControllerId() {
        return controller.getId();
    }

}
