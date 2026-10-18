package io.quarkus.arc.test.observers.inheritance;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 18/07/2019
 */
public class SimpleEvent {
    public final String content;

    public SimpleEvent(String content) {
        this.content = content;
    }
}
