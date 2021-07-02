package io.quarkus.it.resteasy.reactive;

import java.sql.Date;
import java.time.LocalDate;

public class Greeting {

    private final String message;
    private final LocalDate date;
    private final Date sqlDate;

    public Greeting(String message, LocalDate date, Date sqlDate) {
        this.message = message;
        this.date = date;
        this.sqlDate = sqlDate;
    }

    public String getMessage() {
        return message;
    }

    public LocalDate getDate() {
        return date;
    }

    public Date getSqlDate() {
        return sqlDate;
    }
}
