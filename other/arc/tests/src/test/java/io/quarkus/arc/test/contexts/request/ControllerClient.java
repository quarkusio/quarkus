package io.quarkus.arc.test.contexts.request;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

@ApplicationScoped
public class ControllerClient {

    @Inject
    Controller controller;

    @ActivateRequestContext
    String getControllerId() {
        return controller.getId();
    }

}
