package io.quarkus.arc.test.observers.inheritance;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 18/07/2019
 */
@Singleton
public class EmittingBean {

    public static final String VALUE = "my-event";

    @Inject
    Event<SimpleEvent> emitter;

    public void trigger() {
        emitter.fire(new SimpleEvent(VALUE));
    }
}
