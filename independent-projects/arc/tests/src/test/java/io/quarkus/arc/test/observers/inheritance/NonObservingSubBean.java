package io.quarkus.arc.test.observers.inheritance;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 18/07/2019
 */
@ApplicationScoped
public class NonObservingSubBean extends ObservingBean {

    public void watchFor(SimpleEvent event) {
        value = event.content;
    }
}
