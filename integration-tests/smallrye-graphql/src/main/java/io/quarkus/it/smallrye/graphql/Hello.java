package io.quarkus.it.smallrye.graphql;

import java.time.LocalTime;

public class Hello extends Greeting {

    private TimeOfDay timeOfDay = TimeOfDay.ANY;

    public Hello() {
        super("Hello", LocalTime.now());
    }

    public TimeOfDay getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(TimeOfDay timeOfDay) {
        this.timeOfDay = timeOfDay;
    }
}
