package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

public class DateStringShapeNoPatternBean {

    private String name;

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Date utcDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "Europe/Prague")
    private Date pragueDate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getUtcDate() {
        return utcDate;
    }

    public void setUtcDate(Date utcDate) {
        this.utcDate = utcDate;
    }

    public Date getPragueDate() {
        return pragueDate;
    }

    public void setPragueDate(Date pragueDate) {
        this.pragueDate = pragueDate;
    }
}
