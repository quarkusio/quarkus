package io.quarkus.deployment.dev.annotation_dependent_classes.model;

import java.util.Calendar;
import java.util.Date;

public abstract class MapperHelper {
    Calendar mapToCalendar(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return calendar;
    }
}
