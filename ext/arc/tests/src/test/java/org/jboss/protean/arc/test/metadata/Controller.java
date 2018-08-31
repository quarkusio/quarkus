package org.jboss.protean.arc.test.metadata;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;

@Dependent
public class Controller {

    @Inject
    Bean<Controller> bean;

}
