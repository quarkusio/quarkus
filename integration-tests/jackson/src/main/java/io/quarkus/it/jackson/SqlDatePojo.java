package io.quarkus.it.jackson;

import java.sql.Date;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class SqlDatePojo {

    public Date date;

    public SqlDatePojo() {
    }

    public SqlDatePojo(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
