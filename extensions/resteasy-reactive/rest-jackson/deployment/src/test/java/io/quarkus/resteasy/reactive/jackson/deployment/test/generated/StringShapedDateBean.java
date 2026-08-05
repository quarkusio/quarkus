package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

public class StringShapedDateBean {

    private String name;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Date date;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
