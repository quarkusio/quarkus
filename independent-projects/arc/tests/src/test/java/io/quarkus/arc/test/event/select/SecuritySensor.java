package io.quarkus.arc.test.event.select;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

@Dependent
public class SecuritySensor {

    @Inject
    @Any
    Event<SecurityEvent> securityEvent;
}
