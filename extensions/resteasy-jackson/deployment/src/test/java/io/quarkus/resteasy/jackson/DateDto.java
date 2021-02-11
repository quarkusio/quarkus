package io.quarkus.resteasy.jackson;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DateDto {
    @JsonProperty("current_date")
    private ZonedDateTime currentDate;

    public void setCurrentDate(ZonedDateTime currentDate) {
        this.currentDate = currentDate;
    }

    public ZonedDateTime getCurrentDate() {
        return currentDate;
    }

    public DateDto(ZonedDateTime currentDate) {
        this.currentDate = currentDate;
    }
}
