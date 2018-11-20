package org.jboss.shamrock.arc.runtime;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.shamrock.runtime.ShutdownEvent;
import org.jboss.shamrock.runtime.StartupEvent;

/**
 *
 */
@Dependent
public class LifecycleEventRunner {

    @Inject
    Event<StartupEvent> startup;


    @Inject
    Event<ShutdownEvent> shutdown;


    public void fireStartupEvent() {
        startup.fire(new StartupEvent());
    }

    public void fireShutdownEvent() {
        shutdown.fire(new ShutdownEvent());
    }

}
