package io.quarkus.it.smallrye.graphql;

import java.time.LocalTime;

public class Morning extends Greeting {

    private TimeOfDay timeOfDay = TimeOfDay.MORNING;

    public Morning() {
        super("Good Morning", LocalTime.now());
    }

    public TimeOfDay getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(TimeOfDay timeOfDay) {
        this.timeOfDay = timeOfDay;
    }
}
