package org.jboss.shamrock.arc.runtime;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.shamrock.runtime.StartupEvent;

/**
 * TODO: this should not be necessary
 */
@Dependent
public class StartupEventRunner {

    @Inject
    private Event<StartupEvent> event;

    public void fireEvent() {
        event.fire(new StartupEvent());
    }

}
