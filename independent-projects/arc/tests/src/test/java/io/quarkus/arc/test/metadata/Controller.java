package io.quarkus.arc.test.metadata;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;

@Dependent
public class Controller {

    @Inject
    Bean<Controller> bean;

}
