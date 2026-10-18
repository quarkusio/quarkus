package io.quarkus.arc.test.event.select;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

@Dependent
public class SecuritySensor {

    @Inject
    @Any
    Event<SecurityEvent> securityEvent;
}
