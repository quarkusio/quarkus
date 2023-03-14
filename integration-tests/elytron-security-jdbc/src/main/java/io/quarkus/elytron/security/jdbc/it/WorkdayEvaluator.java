package io.quarkus.elytron.security.jdbc.it;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;

@Unremovable
@ApplicationScoped
public class WorkdayEvaluator {

    private static final Set<String> WORKDAYS = Set.of("Monday", "Tuesday", "Wednesday", "Thrusday", "Friday");

    public boolean isWorkday(String day) {
        return WORKDAYS.contains(day);
    }

}
