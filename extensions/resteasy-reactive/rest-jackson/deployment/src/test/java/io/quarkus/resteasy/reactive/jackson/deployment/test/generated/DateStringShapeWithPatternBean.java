package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

public class DateStringShapeWithPatternBean {

    private String name;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Date directDate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDirectDate() {
        return directDate;
    }

    public void setDirectDate(Date directDate) {
        this.directDate = directDate;
    }
}
