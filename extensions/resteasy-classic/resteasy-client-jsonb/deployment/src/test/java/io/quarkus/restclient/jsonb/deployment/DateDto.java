package io.quarkus.restclient.jsonb.deployment;

import java.time.ZonedDateTime;

public class DateDto {

    private ZonedDateTime date;

    public DateDto() {
    }

    public DateDto(ZonedDateTime date) {
        this.date = date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public ZonedDateTime getDate() {
        return date;
    }
}
